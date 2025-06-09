package de.iske.kistogramm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.ImportResult;
import de.iske.kistogramm.dto.export.*;
import de.iske.kistogramm.exception.ImportException;
import de.iske.kistogramm.model.*;
import de.iske.kistogramm.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ImportService {

  private final CategoryRepository categoryRepository;
  private final CategoryAttributeTemplateRepository categoryAttributeTemplateRepository;
  private final ImageRepository imageRepository;
  private final ItemRepository itemRepository;
  private final RoomRepository roomRepository;
  private final StorageRepository storageRepository;
  private final TagRepository tagRepository;

  private final ObjectMapper objectMapper;

  private static final Logger LOG = LoggerFactory.getLogger(ImportService.class);

  public ImportService(
          CategoryRepository categoryRepository,
          CategoryAttributeTemplateRepository categoryAttributeTemplateRepository,
          ImageRepository imageRepository,
          ItemRepository itemRepository,
          RoomRepository roomRepository,
          StorageRepository storageRepository,
          TagRepository tagRepository,
          ObjectMapper objectMapper) {
    this.categoryRepository = categoryRepository;
    this.categoryAttributeTemplateRepository = categoryAttributeTemplateRepository;
    this.imageRepository = imageRepository;
    this.itemRepository = itemRepository;
    this.roomRepository = roomRepository;
    this.storageRepository = storageRepository;
    this.tagRepository = tagRepository;
    this.objectMapper = objectMapper;
  }

  public ImportResult importArchive(MultipartFile file, boolean overwrite, boolean failOnError) throws IOException {
    var importResult = new ImportResult();

    importResult.setOverwriteMode(overwrite);
    importResult.setSuccess(false);
    importResult.setErrors(new ArrayList<>());
    importResult.setWarnings(new ArrayList<>());

    var files = extractFiles(file);
    var dataToImport = parseDataJson(files.get("data.json"), importResult);

    try {

      checkForExistingCategoryNames(
              dataToImport.getCategories(),
              dataToImport.getItems(),
              dataToImport.getCategoryAttributeTemplates(),
              importResult);

      var images = importImages(dataToImport.getImages(), files, failOnError, importResult);
      var rooms = importRooms(dataToImport.getRooms(), images, failOnError, importResult);
      var storages = importStorages(dataToImport.getStorages(), rooms, images, failOnError, importResult);
      var categories = importCategories(dataToImport.getCategories(), failOnError, importResult);
      var tags = importTags(dataToImport.getTags(), failOnError, importResult);
      importCategoryAttributeTemplates(dataToImport.getCategoryAttributeTemplates(), categories, failOnError, importResult);
      var items = importItems(dataToImport.getItems(), categories, storages, tags, images, failOnError, importResult);
      linkRelatedItems(dataToImport.getItems(), items);
    } catch (ImportException _) {
      if (failOnError) {
        importResult.setSuccess(false);
        return importResult;
      }
    }

    importResult.setSuccess(importResult.getErrors().isEmpty());
    return importResult;
  }

  /**
   * Checks if categories already exist with their names in the database.
   * If overwrite mode is enabled, it will update existing categories and also replace the existing UUID.
   * if not in overwrite mode the existing uuid remains and the reference in the import data is replaced with the existing uuid.
   *
   * @param categories
   * @param importResult
   */
  private void checkForExistingCategoryNames(List<ExportCategory> categories,
                                             List<ExportItem> items,
                                             List<ExportCategoryAttributeTemplate> templates,
                                             ImportResult importResult) {
    // get names of categories that should be imported
    for (var catToImport : categories) {
      var existingCat = categoryRepository.findByName(catToImport.getName());
      // if they both have the same uuid, we can skip this check
      if (existingCat.isPresent() && existingCat.get().getUuid().equals(catToImport.getUuid())) {
        continue;
      }
      if (existingCat.isPresent() && !importResult.isOverwriteMode()) {
        // Replace the uuid in the import data with the existing category uuid
        LOG.warn("Category with name '{}' already exists with UUID '{}', replacing in import data with existing UUID",
                catToImport.getName(), existingCat.get().getUuid());
        items.forEach(item -> {
          if (item.getCategory() != null && item.getCategory().equals(catToImport.getUuid())) {
            item.setCategory(existingCat.get().getUuid());
          }
        });
        templates.forEach(template -> {
          if (template.getCategory().equals(catToImport.getUuid())) {
            template.setCategory(existingCat.get().getUuid());
          }
        });
      }
    }
  }

  private Map<String, byte[]> extractFiles(MultipartFile file) throws IOException {
    Map<String, byte[]> files = new HashMap<>();
    try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        zis.transferTo(baos);
        files.put(entry.getName(), baos.toByteArray());
      }
    }
    return files;
  }

  private ExportResult parseDataJson(byte[] jsonBytes, ImportResult importResult) throws IOException {
    if (jsonBytes == null) {
      importResult.getErrors().add("data.json missing in archive");
      throw new ImportException(importResult);
    }
    return objectMapper.readValue(jsonBytes, ExportResult.class);
  }

  private Map<UUID, ImageEntity> importImages(List<ExportImage> imagesToImport,
                                              Map<String, byte[]> files,
                                              boolean failOnError,
                                              ImportResult result) throws ImportException {
    Map<UUID, ImageEntity> map = new HashMap<>();
    if (imagesToImport == null) {
      return map;
    }
    for (var imageToImport : imagesToImport) {
      try {
        Optional<ImageEntity> existing = imageRepository.findByUuid(imageToImport.getUuid());
        ImageEntity entity;
        if (existing.isPresent()) {
          if (!result.isOverwriteMode()) {
            result.setSkippedTotalCount(result.getSkippedTotalCount() + 1);
            map.put(imageToImport.getUuid(), existing.get());
            continue;
          }
          entity = existing.get();
          result.setUpdatedTotalCount(result.getUpdatedTotalCount() + 1);
        } else {
          entity = new ImageEntity();
          result.setImportedImageCount(result.getImportedImageCount() + 1);
          result.setImportedTotalCount(result.getImportedTotalCount() + 1);
        }
        entity.setUuid(imageToImport.getUuid());
        entity.setDescription(imageToImport.getDescription());
        entity.setType(imageToImport.getType());
        entity.setDateAdded(imageToImport.getDateAdded() != null ? imageToImport.getDateAdded() : LocalDateTime.now());
        entity.setDateModified(imageToImport.getDateModified() != null ? imageToImport.getDateModified() : LocalDateTime.now());
        byte[] data = files.get("images/" + imageToImport.getUuid());
        entity.setData(data != null ? data : new byte[0]);
        map.put(imageToImport.getUuid(), imageRepository.save(entity));
      } catch (Exception e) {
        result.getErrors().add("Failed to import image " + imageToImport.getUuid() + ": " + e.getMessage());
        result.setFailedTotalCount(result.getFailedTotalCount() + 1);
        if (failOnError) {
          throw new ImportException(result);
        }
      }
    }
    return map;
  }

  private Map<UUID, RoomEntity> importRooms(List<ExportRoom> roomsToImport,
                                            Map<UUID, ImageEntity> images,
                                            boolean failOnError,
                                            ImportResult result) throws ImportException {
    Map<UUID, RoomEntity> map = new HashMap<>();
    if (roomsToImport == null) {
      return map;
    }
    for (var roomToImport : roomsToImport) {
      try {
        Optional<RoomEntity> existing = roomRepository.findByUuid(roomToImport.getUuid());
        RoomEntity entity;
        if (existing.isPresent()) {
          if (!result.isOverwriteMode()) {
            result.setSkippedTotalCount(result.getSkippedTotalCount() + 1);
            map.put(roomToImport.getUuid(), existing.get());
            continue;
          }
          entity = existing.get();
          result.setUpdatedRoomCount(result.getUpdatedRoomCount() + 1);
          result.setUpdatedTotalCount(result.getUpdatedTotalCount() + 1);
        } else {
          entity = new RoomEntity();
          result.setImportedRoomCount(result.getImportedRoomCount() + 1);
          result.setImportedTotalCount(result.getImportedTotalCount() + 1);
        }
        entity.setUuid(roomToImport.getUuid());
        entity.setName(roomToImport.getName());
        entity.setDescription(roomToImport.getDescription());
        entity.setDateAdded(roomToImport.getDateAdded() != null ? roomToImport.getDateAdded() : LocalDateTime.now());
        entity.setDateModified(roomToImport.getDateModified() != null ? roomToImport.getDateModified() : LocalDateTime.now());
        if (roomToImport.getImage() != null) {
          ImageEntity img = images.get(roomToImport.getImage());
          if (img != null) {
            entity.setImage(img);
            img.setRoom(entity);
          }
        }
        map.put(roomToImport.getUuid(), roomRepository.save(entity));
      } catch (Exception e) {
        result.getErrors().add("Failed to import room " + roomToImport.getUuid() + ": " + e.getMessage());
        result.setFailedTotalCount(result.getFailedTotalCount() + 1);
        if (failOnError) {
          throw new ImportException(result);
        }
      }
    }
    return map;
  }

  private Map<UUID, StorageEntity> importStorages(List<ExportStorage> storagesToImport,
                                                  Map<UUID, RoomEntity> rooms,
                                                  Map<UUID, ImageEntity> images,
                                                  boolean failOnError,
                                                  ImportResult result) throws ImportException {
    Map<UUID, StorageEntity> map = new HashMap<>();
    Map<StorageEntity, UUID> pendingParents = new HashMap<>();
    if (storagesToImport == null) {
      return map;
    }
    for (var storageToImport : storagesToImport) {
      try {
        Optional<StorageEntity> existing = storageRepository.findByUuid(storageToImport.getUuid());
        StorageEntity entity;
        if (existing.isPresent()) {
          if (!result.isOverwriteMode()) {
            result.setSkippedTotalCount(result.getSkippedTotalCount() + 1);
            map.put(storageToImport.getUuid(), existing.get());
            continue;
          }
          entity = existing.get();
          result.setUpdatedStorageCount(result.getUpdatedStorageCount() + 1);
          result.setUpdatedTotalCount(result.getUpdatedTotalCount() + 1);
        } else {
          entity = new StorageEntity();
          result.setImportedStorageCount(result.getImportedStorageCount() + 1);
          result.setImportedTotalCount(result.getImportedTotalCount() + 1);
        }

        entity.setUuid(storageToImport.getUuid());
        entity.setName(storageToImport.getName());
        entity.setDescription(storageToImport.getDescription());
        entity.setDateAdded(storageToImport.getDateAdded() != null ? storageToImport.getDateAdded() : LocalDateTime.now());
        entity.setDateModified(storageToImport.getDateModified() != null ? storageToImport.getDateModified() : LocalDateTime.now());
        if (storageToImport.getRoom() != null) {
          entity.setRoom(rooms.get(storageToImport.getRoom()));
        }
        if (storageToImport.getParentStorage() != null) {
          pendingParents.put(entity, storageToImport.getParentStorage());
        }
        StorageEntity saved = storageRepository.save(entity);
        map.put(storageToImport.getUuid(), saved);
        if (storageToImport.getImages() != null) {
          for (UUID imgUuid : storageToImport.getImages()) {
            ImageEntity img = images.get(imgUuid);
            if (img != null) {
              img.setStorage(saved);
              imageRepository.save(img);
            }
          }
        }
      } catch (Exception e) {
        result.getErrors().add("Failed to import storage " + storageToImport.getUuid());
        result.setFailedTotalCount(result.getFailedTotalCount() + 1);
        if (failOnError) {
          throw new ImportException(result);
        }
      }
    }
    // resolve parent references after all storages are saved
    for (Map.Entry<StorageEntity, UUID> entry : pendingParents.entrySet()) {
      StorageEntity child = entry.getKey();
      UUID parentUuid = entry.getValue();
      StorageEntity parent = map.get(parentUuid);
      if (parent == null) {
        parent = storageRepository.findByUuid(parentUuid).orElse(null);
      }
      if (parent != null) {
        child.setParentStorage(parent);
        storageRepository.save(child);
      } else {
        result.getWarnings().add("Parent storage " + parentUuid + " for storage " + child.getUuid() + " not found");
      }
    }
    return map;
  }

  private Map<UUID, CategoryEntity> importCategories(List<ExportCategory> categoriesToImport,
                                                     boolean failOnError,
                                                     ImportResult result) throws ImportException {
    Map<UUID, CategoryEntity> importedCategories = new HashMap<>();
    if (categoriesToImport == null) {
      return importedCategories; // return empty map if no categories to import
    }
    for (var categoryToImport : categoriesToImport) {
      try {
        CategoryEntity entity;
        // Check if category already exists by UUID
        Optional<CategoryEntity> existing = categoryRepository.findByUuid(categoryToImport.getUuid());
        // if existing and overwrite mode, we will update it
        if (existing.isPresent() // category with UUID exists
                && (!existing.get().getName().equals(categoryToImport.getName()) // name has changed OR
                || !Optional.ofNullable(existing.get().getDescription()).orElse("")
                .equals(categoryToImport.getDescription())) // description has changed
                && result.isOverwriteMode()) { // and we are in overwrite mode
          LOG.info("Updating name and description for existing category with UUID '{}': {} -> {}",
                  categoryToImport.getUuid(),
                  existing.get().getName(),
                  categoryToImport.getName());
          entity = existing.get();
          // Update the name if it has changed
          entity.setName(categoryToImport.getName());
          entity.setDescription(categoryToImport.getDescription());

          importedCategories.put(categoryToImport.getUuid(), categoryRepository.save(entity));

          result.setUpdatedCategoryCount(result.getUpdatedCategoryCount() + 1);
          result.setUpdatedTotalCount(result.getUpdatedTotalCount() + 1);
          continue;
        }

        // Check if category already exists by name
        existing = categoryRepository.findByName(categoryToImport.getName());
        if (existing.isPresent()) { // category with name exists
          LOG.warn("Category with name '{}' already exists with UUID '{}', but trying to import with UUID '{}'",
                  categoryToImport.getName(),
                  existing.get().getUuid(),
                  categoryToImport.getUuid());
          if (result.isOverwriteMode()) {
            // Update the existing category with the new UUID and other details
            LOG.info("Overwriting existing category ({})", existing.get().getUuid());
            entity = existing.get();
            entity.setUuid(categoryToImport.getUuid());
            entity.setDescription(categoryToImport.getDescription());
            importedCategories.put(categoryToImport.getUuid(), categoryRepository.save(entity));
            result.setUpdatedCategoryCount(result.getUpdatedCategoryCount() + 1);
            continue;
          } else {
            // If not in overwrite mode, skip this category
            // The items has been updated to reference the existing category UUID before
            result.setSkippedTotalCount(result.getSkippedTotalCount() + 1);
            importedCategories.put(existing.get().getUuid(), existing.get());
            continue;
          }
        }

        // If not found, create a new category
        entity = new CategoryEntity();

        entity.setUuid(categoryToImport.getUuid());
        entity.setDescription(categoryToImport.getDescription());
        entity.setName(categoryToImport.getName());
        entity.setDateAdded(categoryToImport.getDateAdded() != null ? categoryToImport.getDateAdded() : LocalDateTime.now());
        entity.setDateModified(categoryToImport.getDateModified() != null ? categoryToImport.getDateModified() : LocalDateTime.now());
        importedCategories.put(categoryToImport.getUuid(), categoryRepository.save(entity));

        result.setImportedCategoryCount(result.getImportedCategoryCount() + 1);
        result.setImportedTotalCount(result.getImportedTotalCount() + 1);

        importedCategories.put(categoryToImport.getUuid(), categoryRepository.save(entity));
      } catch (Exception e) {
        result.getErrors().add("Failed to import category " + categoryToImport.getUuid() + ": " + e.getMessage());
        result.setFailedTotalCount(result.getFailedTotalCount() + 1);
        if (failOnError) {
          throw new ImportException(result);
        }
      }
    }
    return importedCategories;
  }

  private Map<UUID, TagEntity> importTags(List<ExportTag> tagsToImport,
                                          boolean failOnError,
                                          ImportResult result) throws ImportException {
    Map<UUID, TagEntity> map = new HashMap<>();
    if (tagsToImport == null) {
      return map;
    }
    for (var tagToImport : tagsToImport) {
      try {
        Optional<TagEntity> existing = tagRepository.findByUuid(tagToImport.getUuid());
        TagEntity entity;
        if (existing.isPresent()) {
          if (!result.isOverwriteMode()) {
            result.setSkippedTotalCount(result.getSkippedTotalCount() + 1);
            map.put(tagToImport.getUuid(), existing.get());
            continue;
          }
          entity = existing.get();
          result.setUpdatedTagCount(result.getUpdatedTagCount() + 1);
          result.setUpdatedTotalCount(result.getUpdatedTotalCount() + 1);
        } else {
          entity = new TagEntity();
          result.setImportedTagCount(result.getImportedTagCount() + 1);
          result.setImportedTotalCount(result.getImportedTotalCount() + 1);
        }

        entity.setUuid(tagToImport.getUuid());
        entity.setName(tagToImport.getName());
        entity.setDateAdded(tagToImport.getDateAdded() != null ? tagToImport.getDateAdded() : LocalDateTime.now());
        entity.setDateModified(tagToImport.getDateModified() != null ? tagToImport.getDateModified() : LocalDateTime.now());
        map.put(tagToImport.getUuid(), tagRepository.save(entity));
      } catch (Exception e) {
        result.getErrors().add("Failed to import tag " + tagToImport.getUuid() + ": " + e.getMessage());
        result.setFailedTotalCount(result.getFailedTotalCount() + 1);
        if (failOnError) {
          throw new ImportException(result);
        }
      }
    }
    return map;
  }

  private void importCategoryAttributeTemplates(List<ExportCategoryAttributeTemplate> templatesToImport,
                                                Map<UUID, CategoryEntity> categories,
                                                boolean failOnError,
                                                ImportResult result) throws ImportException {
    if (templatesToImport == null) {
      return;
    }
    for (var templateToImport : templatesToImport) {
      CategoryEntity cat = categories.get(templateToImport.getCategory());
      if (cat == null) {
        result.getWarnings().add("Category for template " + templateToImport.getUuid() + " not found");
        continue;
      }
      try {
        Optional<CategoryAttributeTemplateEntity> existing = categoryAttributeTemplateRepository.findByUuid(templateToImport.getUuid());
        CategoryAttributeTemplateEntity entity;
        if (existing.isPresent()) {
          if (!result.isOverwriteMode()) {
            result.setSkippedTotalCount(result.getSkippedTotalCount() + 1);
            continue;
          }
          entity = existing.get();
          result.setUpdatedCategoryAttributeTemplateCount(result.getUpdatedCategoryAttributeTemplateCount() + 1);
          result.setUpdatedTotalCount(result.getUpdatedTotalCount() + 1);
        } else {
          entity = new CategoryAttributeTemplateEntity();
          result.setImportedCategoryAttributeTemplateCount(result.getImportedCategoryAttributeTemplateCount() + 1);
          result.setImportedTotalCount(result.getImportedTotalCount() + 1);
        }
        entity.setUuid(templateToImport.getUuid());
        entity.setCategory(cat);
        entity.setAttributeName(templateToImport.getAttributeName());
        entity.setDateAdded(templateToImport.getDateAdded() != null ? templateToImport.getDateAdded() : LocalDateTime.now());
        entity.setDateModified(templateToImport.getDateModified() != null ? templateToImport.getDateModified() : LocalDateTime.now());
        categoryAttributeTemplateRepository.save(entity);
      } catch (Exception e) {
        result.getErrors().add("Failed to import category attribute template " + templateToImport.getUuid() + ": " + e.getMessage());
        result.setFailedTotalCount(result.getFailedTotalCount() + 1);
        if (failOnError) {
          throw new ImportException(result);
        }
      }
    }
  }

  private Map<UUID, ItemEntity> importItems(List<ExportItem> itemsToImport,
                                            Map<UUID, CategoryEntity> categories,
                                            Map<UUID, StorageEntity> storages,
                                            Map<UUID, TagEntity> tags,
                                            Map<UUID, ImageEntity> images,
                                            boolean failOnError,
                                            ImportResult result) throws ImportException {
    Map<UUID, ItemEntity> map = new HashMap<>();
    if (itemsToImport == null) {
      return map;
    }
    for (var itemToImport : itemsToImport) {
      try {
        Optional<ItemEntity> existing = itemRepository.findByUuid(itemToImport.getUuid());
        ItemEntity entity;
        if (existing.isPresent()) {
          if (!result.isOverwriteMode()) {
            result.setSkippedTotalCount(result.getSkippedTotalCount() + 1);
            map.put(itemToImport.getUuid(), existing.get());
            continue;
          }
          entity = existing.get();
          result.setUpdatedItemCount(result.getUpdatedItemCount() + 1);
          result.setUpdatedTotalCount(result.getUpdatedTotalCount() + 1);
        } else {
          entity = new ItemEntity();
          result.setImportedItemCount(result.getImportedItemCount() + 1);
          result.setImportedTotalCount(result.getImportedTotalCount() + 1);
        }

        entity.setUuid(itemToImport.getUuid());
        entity.setName(itemToImport.getName());
        entity.setDescription(itemToImport.getDescription());
        entity.setPurchaseDate(itemToImport.getPurchaseDate());
        entity.setPurchasePrice(itemToImport.getPurchasePrice());
        entity.setQuantity(itemToImport.getQuantity());
        entity.setDateAdded(itemToImport.getDateAdded() != null ? itemToImport.getDateAdded() : LocalDateTime.now());
        entity.setDateModified(itemToImport.getDateModified() != null ? itemToImport.getDateModified() : LocalDateTime.now());
        if (itemToImport.getCategory() != null) {
          entity.setCategory(categories.get(itemToImport.getCategory()));
        }
        if (itemToImport.getStorage() != null) {
          entity.setStorage(storages.get(itemToImport.getStorage()));
        }
        if (itemToImport.getTags() != null) {
          Set<TagEntity> tagSet = new HashSet<>();
          for (UUID uuid : itemToImport.getTags()) {
            TagEntity t = tags.get(uuid);
            if (t != null) {
              tagSet.add(t);
            }
          }
          entity.setTags(tagSet);
        }
        ItemEntity saved = itemRepository.save(entity);
        map.put(itemToImport.getUuid(), saved);
        if (itemToImport.getImages() != null) {
          for (UUID imgUuid : itemToImport.getImages()) {
            ImageEntity img = images.get(imgUuid);
            if (img != null) {
              img.setItem(saved);
              imageRepository.save(img);
            }
          }
        }
      } catch (Exception e) {
        result.getErrors().add("Failed to import item " + itemToImport.getUuid() + ": " + e.getMessage());
        result.setFailedTotalCount(result.getFailedTotalCount() + 1);
        if (failOnError) {
          throw new ImportException(result);
        }
      }
    }
    return map;
  }

  private void linkRelatedItems(List<ExportItem> items, Map<UUID, ItemEntity> itemToUuid) {
    if (items == null) {
      return;
    }
    for (ExportItem exp : items) {
      if (exp.getRelatedItems() == null) {
        continue;
      }
      ItemEntity entity = itemToUuid.get(exp.getUuid());
      if (entity == null) {
        continue;
      }
      Set<ItemEntity> related = new HashSet<>();
      for (UUID uuid : exp.getRelatedItems()) {
        ItemEntity rel = itemToUuid.get(uuid);
        if (rel != null) {
          related.add(rel);
        }
      }
      entity.setRelatedItems(related);
      itemRepository.save(entity);
    }
  }
}

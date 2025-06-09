package de.iske.kistogramm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.ImportResult;
import de.iske.kistogramm.dto.export.*;
import de.iske.kistogramm.model.*;
import de.iske.kistogramm.repository.*;
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
    ImportResult importResult = new ImportResult();
    importResult.setOverwriteMode(overwrite);
    importResult.setSuccess(false);
    importResult.setErrors(new ArrayList<>());
    importResult.setWarnings(new ArrayList<>());

    Map<String, byte[]> files = extractFiles(file);
    ExportResult result;
    try {
      result = parseExportResult(files.get("data.json"));
    } catch (IllegalArgumentException e) {
      importResult.getErrors().add(e.getMessage());
      return importResult;
    }

    try {
      Map<UUID, ImageEntity> images = importImages(result.getImages(), files, overwrite, failOnError, importResult);
      Map<UUID, RoomEntity> rooms = importRooms(result.getRooms(), images, overwrite, failOnError, importResult);
      Map<UUID, StorageEntity> storages = importStorages(result.getStorages(), rooms, images, overwrite, failOnError, importResult);
      Map<UUID, CategoryEntity> categories = importCategories(result.getCategories(), overwrite, failOnError, importResult);
      Map<UUID, TagEntity> tags = importTags(result.getTags(), overwrite, failOnError, importResult);
      importCategoryAttributeTemplates(result.getCategoryAttributeTemplates(), categories, overwrite, failOnError, importResult);
      Map<UUID, ItemEntity> items = importItems(result.getItems(), categories, storages, tags, images, overwrite, failOnError, importResult);
      linkRelatedItems(result.getItems(), items);
    } catch (RuntimeException ex) {
      if (failOnError) {
        importResult.setSuccess(false);
        return importResult;
      }
    }

    importResult.setSuccess(importResult.getErrors().isEmpty());
    return importResult;
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

  private ExportResult parseExportResult(byte[] jsonBytes) throws IOException {
    if (jsonBytes == null) {
      throw new IllegalArgumentException("data.json missing in archive");
    }
    return objectMapper.readValue(jsonBytes, ExportResult.class);
  }

  private Map<UUID, ImageEntity> importImages(List<ExportImage> exportImages,
                                             Map<String, byte[]> files,
                                             boolean overwrite,
                                             boolean failOnError,
                                             ImportResult result) {
    Map<UUID, ImageEntity> map = new HashMap<>();
    if (exportImages == null) {
      return map;
    }
    for (ExportImage exp : exportImages) {
      try {
        Optional<ImageEntity> existing = imageRepository.findByUuid(exp.getUuid());
        ImageEntity entity;
        if (existing.isPresent()) {
          if (!overwrite) {
            result.setSkippedTotalCount(result.getSkippedTotalCount() + 1);
            map.put(exp.getUuid(), existing.get());
            continue;
          }
          entity = existing.get();
          result.setUpdatedTotalCount(result.getUpdatedTotalCount() + 1);
        } else {
          entity = new ImageEntity();
          result.setImportedImageCount(result.getImportedImageCount() + 1);
          result.setImportedTotalCount(result.getImportedTotalCount() + 1);
        }
        entity.setUuid(exp.getUuid());
        entity.setDescription(exp.getDescription());
        entity.setDateAdded(exp.getDateAdded() != null ? exp.getDateAdded() : LocalDateTime.now());
        entity.setDateModified(exp.getDateModified() != null ? exp.getDateModified() : LocalDateTime.now());
        byte[] data = files.get("images/" + exp.getUuid());
        entity.setData(data != null ? data : new byte[0]);
        map.put(exp.getUuid(), imageRepository.save(entity));
      } catch (Exception e) {
        result.getErrors().add("Failed to import image " + exp.getUuid());
        result.setFailedTotalCount(result.getFailedTotalCount() + 1);
        if (failOnError) {
          throw new RuntimeException(e);
        }
      }
    }
    return map;
  }

  private Map<UUID, RoomEntity> importRooms(List<ExportRoom> exportRooms,
                                            Map<UUID, ImageEntity> images,
                                            boolean overwrite,
                                            boolean failOnError,
                                            ImportResult result) {
    Map<UUID, RoomEntity> map = new HashMap<>();
    if (exportRooms == null) {
      return map;
    }
    for (ExportRoom exp : exportRooms) {
      try {
        Optional<RoomEntity> existing = roomRepository.findByUuid(exp.getUuid());
        RoomEntity entity;
        if (existing.isPresent()) {
          if (!overwrite) {
            result.setSkippedTotalCount(result.getSkippedTotalCount() + 1);
            map.put(exp.getUuid(), existing.get());
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
        entity.setUuid(exp.getUuid());
        entity.setName(exp.getName());
        entity.setDescription(exp.getDescription());
        entity.setDateAdded(exp.getDateAdded() != null ? exp.getDateAdded() : LocalDateTime.now());
        entity.setDateModified(exp.getDateModified() != null ? exp.getDateModified() : LocalDateTime.now());
        if (exp.getImage() != null) {
          ImageEntity img = images.get(exp.getImage());
          if (img != null) {
            entity.setImage(img);
            img.setRoom(entity);
          }
        }
        map.put(exp.getUuid(), roomRepository.save(entity));
      } catch (Exception e) {
        result.getErrors().add("Failed to import room " + exp.getUuid());
        result.setFailedTotalCount(result.getFailedTotalCount() + 1);
        if (failOnError) {
          throw new RuntimeException(e);
        }
      }
    }
    return map;
  }

  private Map<UUID, StorageEntity> importStorages(List<ExportStorage> exportStorages,
                                                  Map<UUID, RoomEntity> rooms,
                                                  Map<UUID, ImageEntity> images,
                                                  boolean overwrite,
                                                  boolean failOnError,
                                                  ImportResult result) {
    Map<UUID, StorageEntity> map = new HashMap<>();
    Map<StorageEntity, UUID> pendingParents = new HashMap<>();
    if (exportStorages == null) {
      return map;
    }
    for (ExportStorage exp : exportStorages) {
      try {
        Optional<StorageEntity> existing = storageRepository.findByUuid(exp.getUuid());
        StorageEntity entity;
        if (existing.isPresent()) {
          if (!overwrite) {
            result.setSkippedTotalCount(result.getSkippedTotalCount() + 1);
            map.put(exp.getUuid(), existing.get());
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

        entity.setUuid(exp.getUuid());
        entity.setName(exp.getName());
        entity.setDescription(exp.getDescription());
        entity.setDateAdded(exp.getDateAdded() != null ? exp.getDateAdded() : LocalDateTime.now());
        entity.setDateModified(exp.getDateModified() != null ? exp.getDateModified() : LocalDateTime.now());
        if (exp.getRoom() != null) {
          entity.setRoom(rooms.get(exp.getRoom()));
        }
        if (exp.getParentStorage() != null) {
          pendingParents.put(entity, exp.getParentStorage());
        }
        StorageEntity saved = storageRepository.save(entity);
        map.put(exp.getUuid(), saved);
        if (exp.getImages() != null) {
          for (UUID imgUuid : exp.getImages()) {
            ImageEntity img = images.get(imgUuid);
            if (img != null) {
              img.setStorage(saved);
              imageRepository.save(img);
            }
          }
        }
      } catch (Exception e) {
        result.getErrors().add("Failed to import storage " + exp.getUuid());
        result.setFailedTotalCount(result.getFailedTotalCount() + 1);
        if (failOnError) {
          throw new RuntimeException(e);
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

  private Map<UUID, CategoryEntity> importCategories(List<ExportCategory> exportCategories,
                                                     boolean overwrite,
                                                     boolean failOnError,
                                                     ImportResult result) {
    Map<UUID, CategoryEntity> map = new HashMap<>();
    if (exportCategories == null) {
      return map;
    }
    for (ExportCategory exp : exportCategories) {
      try {
        Optional<CategoryEntity> existing = categoryRepository.findByUuid(exp.getUuid());
        CategoryEntity entity;
        if (existing.isPresent()) {
          if (!overwrite) {
            result.setSkippedTotalCount(result.getSkippedTotalCount() + 1);
            map.put(exp.getUuid(), existing.get());
            continue;
          }
          entity = existing.get();
          result.setUpdatedCategoryCount(result.getUpdatedCategoryCount() + 1);
          result.setUpdatedTotalCount(result.getUpdatedTotalCount() + 1);
        } else {
          entity = new CategoryEntity();
          result.setImportedCategoryCount(result.getImportedCategoryCount() + 1);
          result.setImportedTotalCount(result.getImportedTotalCount() + 1);
        }

        entity.setUuid(exp.getUuid());
        entity.setName(exp.getName());
        entity.setDateAdded(exp.getDateAdded() != null ? exp.getDateAdded() : LocalDateTime.now());
        entity.setDateModified(exp.getDateModified() != null ? exp.getDateModified() : LocalDateTime.now());
        map.put(exp.getUuid(), categoryRepository.save(entity));
      } catch (Exception e) {
        result.getErrors().add("Failed to import category " + exp.getUuid());
        result.setFailedTotalCount(result.getFailedTotalCount() + 1);
        if (failOnError) {
          throw new RuntimeException(e);
        }
      }
    }
    return map;
  }

  private Map<UUID, TagEntity> importTags(List<ExportTag> exportTags,
                                          boolean overwrite,
                                          boolean failOnError,
                                          ImportResult result) {
    Map<UUID, TagEntity> map = new HashMap<>();
    if (exportTags == null) {
      return map;
    }
    for (ExportTag exp : exportTags) {
      try {
        Optional<TagEntity> existing = tagRepository.findByUuid(exp.getUuid());
        TagEntity entity;
        if (existing.isPresent()) {
          if (!overwrite) {
            result.setSkippedTotalCount(result.getSkippedTotalCount() + 1);
            map.put(exp.getUuid(), existing.get());
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

        entity.setUuid(exp.getUuid());
        entity.setName(exp.getName());
        entity.setDateAdded(exp.getDateAdded() != null ? exp.getDateAdded() : LocalDateTime.now());
        entity.setDateModified(exp.getDateModified() != null ? exp.getDateModified() : LocalDateTime.now());
        map.put(exp.getUuid(), tagRepository.save(entity));
      } catch (Exception e) {
        result.getErrors().add("Failed to import tag " + exp.getUuid());
        result.setFailedTotalCount(result.getFailedTotalCount() + 1);
        if (failOnError) {
          throw new RuntimeException(e);
        }
      }
    }
    return map;
  }

  private void importCategoryAttributeTemplates(List<ExportCategoryAttributeTemplate> templates,
                                                Map<UUID, CategoryEntity> categories,
                                                boolean overwrite,
                                                boolean failOnError,
                                                ImportResult result) {
    if (templates == null) {
      return;
    }
    for (ExportCategoryAttributeTemplate exp : templates) {
      CategoryEntity cat = categories.get(exp.getCategory());
      if (cat == null) {
        result.getWarnings().add("Category for template " + exp.getUuid() + " not found");
        continue;
      }
      try {
        Optional<CategoryAttributeTemplateEntity> existing = categoryAttributeTemplateRepository.findByUuid(exp.getUuid());
        CategoryAttributeTemplateEntity entity;
        if (existing.isPresent()) {
          if (!overwrite) {
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
        entity.setUuid(exp.getUuid());
        entity.setCategory(cat);
        entity.setAttributeName(exp.getAttributeName());
        entity.setDateAdded(exp.getDateAdded() != null ? exp.getDateAdded() : LocalDateTime.now());
        entity.setDateModified(exp.getDateModified() != null ? exp.getDateModified() : LocalDateTime.now());
        categoryAttributeTemplateRepository.save(entity);
      } catch (Exception e) {
        result.getErrors().add("Failed to import category attribute template " + exp.getUuid());
        result.setFailedTotalCount(result.getFailedTotalCount() + 1);
        if (failOnError) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  private Map<UUID, ItemEntity> importItems(List<ExportItem> exportItems,
                                            Map<UUID, CategoryEntity> categories,
                                            Map<UUID, StorageEntity> storages,
                                            Map<UUID, TagEntity> tags,
                                            Map<UUID, ImageEntity> images,
                                            boolean overwrite,
                                            boolean failOnError,
                                            ImportResult result) {
    Map<UUID, ItemEntity> map = new HashMap<>();
    if (exportItems == null) {
      return map;
    }
    for (ExportItem exp : exportItems) {
      try {
        Optional<ItemEntity> existing = itemRepository.findByUuid(exp.getUuid());
        ItemEntity entity;
        if (existing.isPresent()) {
          if (!overwrite) {
            result.setSkippedTotalCount(result.getSkippedTotalCount() + 1);
            map.put(exp.getUuid(), existing.get());
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

        entity.setUuid(exp.getUuid());
        entity.setName(exp.getName());
        entity.setDescription(exp.getDescription());
        entity.setPurchaseDate(exp.getPurchaseDate());
        entity.setPurchasePrice(exp.getPurchasePrice());
        entity.setQuantity(exp.getQuantity());
        entity.setDateAdded(exp.getDateAdded() != null ? exp.getDateAdded() : LocalDateTime.now());
        entity.setDateModified(exp.getDateModified() != null ? exp.getDateModified() : LocalDateTime.now());
        if (exp.getCategory() != null) {
          entity.setCategory(categories.get(exp.getCategory()));
        }
        if (exp.getStorage() != null) {
          entity.setStorage(storages.get(exp.getStorage()));
        }
        if (exp.getTags() != null) {
          Set<TagEntity> tagSet = new HashSet<>();
          for (UUID uuid : exp.getTags()) {
            TagEntity t = tags.get(uuid);
            if (t != null) {
              tagSet.add(t);
            }
          }
          entity.setTags(tagSet);
        }
        ItemEntity saved = itemRepository.save(entity);
        map.put(exp.getUuid(), saved);
        if (exp.getImages() != null) {
          for (UUID imgUuid : exp.getImages()) {
            ImageEntity img = images.get(imgUuid);
            if (img != null) {
              img.setItem(saved);
              imageRepository.save(img);
            }
          }
        }
      } catch (Exception e) {
        result.getErrors().add("Failed to import item " + exp.getUuid());
        result.setFailedTotalCount(result.getFailedTotalCount() + 1);
        if (failOnError) {
          throw new RuntimeException(e);
        }
      }
    }
    return map;
  }

  private void linkRelatedItems(List<ExportItem> exportItems, Map<UUID, ItemEntity> items) {
    if (exportItems == null) {
      return;
    }
    for (ExportItem exp : exportItems) {
      if (exp.getRelatedItems() == null) {
        continue;
      }
      ItemEntity entity = items.get(exp.getUuid());
      if (entity == null) {
        continue;
      }
      Set<ItemEntity> related = new HashSet<>();
      for (UUID uuid : exp.getRelatedItems()) {
        ItemEntity rel = items.get(uuid);
        if (rel != null) {
          related.add(rel);
        }
      }
      entity.setRelatedItems(related);
      itemRepository.save(entity);
    }
  }
}

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

    Map<String, byte[]> files = extractFiles(file);
    ExportResult result = parseExportResult(files.get("data.json"));

    Map<UUID, ImageEntity> images = importImages(result.getImages(), files);
    Map<UUID, RoomEntity> rooms = importRooms(result.getRooms(), images);
    Map<UUID, StorageEntity> storages = importStorages(result.getStorages(), rooms, images);
    Map<UUID, CategoryEntity> categories = importCategories(result.getCategories());
    Map<UUID, TagEntity> tags = importTags(result.getTags());
    importCategoryAttributeTemplates(result.getCategoryAttributeTemplates(), categories);
    Map<UUID, ItemEntity> items = importItems(result.getItems(), categories, storages, tags, images);
    linkRelatedItems(result.getItems(), items);


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

  private Map<UUID, ImageEntity> importImages(List<ExportImage> exportImages, Map<String, byte[]> files) {
    Map<UUID, ImageEntity> map = new HashMap<>();
    if (exportImages == null) {
      return map;
    }
    for (ExportImage exp : exportImages) {
      if (imageRepository.findByUuid(exp.getUuid()).isPresent()) {
        continue;
      }
      ImageEntity entity = new ImageEntity();
      entity.setUuid(exp.getUuid());
      entity.setDescription(exp.getDescription());
      entity.setDateAdded(exp.getDateAdded() != null ? exp.getDateAdded() : LocalDateTime.now());
      entity.setDateModified(exp.getDateModified() != null ? exp.getDateModified() : LocalDateTime.now());
      byte[] data = files.get("images/" + exp.getUuid());
      if (data != null) {
        entity.setData(data);
      } else {
        entity.setData(new byte[0]);
      }
      map.put(exp.getUuid(), imageRepository.save(entity));
    }
    return map;
  }

  private Map<UUID, RoomEntity> importRooms(List<ExportRoom> exportRooms, Map<UUID, ImageEntity> images) {
    Map<UUID, RoomEntity> map = new HashMap<>();
    if (exportRooms == null) {
      return map;
    }
    for (ExportRoom exp : exportRooms) {
      RoomEntity entity = roomRepository.findByUuid(exp.getUuid()).orElseGet(RoomEntity::new);
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
    }
    return map;
  }

  private Map<UUID, StorageEntity> importStorages(List<ExportStorage> exportStorages,
                                                  Map<UUID, RoomEntity> rooms,
                                                  Map<UUID, ImageEntity> images) {
    Map<UUID, StorageEntity> map = new HashMap<>();
    if (exportStorages == null) {
      return map;
    }

    // Temporarily store mapping of imported storage entities to their parent UUIDs
    Map<StorageEntity, UUID> parentReferences = new HashMap<>();

    for (ExportStorage exp : exportStorages) {
      StorageEntity entity = storageRepository.findByUuid(exp.getUuid()).orElseGet(StorageEntity::new);
      entity.setUuid(exp.getUuid());
      entity.setName(exp.getName());
      entity.setDescription(exp.getDescription());
      entity.setDateAdded(exp.getDateAdded() != null ? exp.getDateAdded() : LocalDateTime.now());
      entity.setDateModified(exp.getDateModified() != null ? exp.getDateModified() : LocalDateTime.now());
      if (exp.getRoom() != null) {
        entity.setRoom(rooms.get(exp.getRoom()));
      }
      if (exp.getParentStorage() != null) {
        parentReferences.put(entity, exp.getParentStorage());
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
    }

    // After all storages are imported, set up the parent relationships
    for (Map.Entry<StorageEntity, UUID> entry : parentReferences.entrySet()) {
      StorageEntity child = entry.getKey();
      UUID parentUuid = entry.getValue();
      StorageEntity parent = map.get(parentUuid);
      if (parent == null) {
        parent = storageRepository.findByUuid(parentUuid).orElse(null);
      }
      child.setParentStorage(parent);
      storageRepository.save(child);
    }

    return map;
  }

  private Map<UUID, CategoryEntity> importCategories(List<ExportCategory> exportCategories) {
    Map<UUID, CategoryEntity> map = new HashMap<>();
    if (exportCategories == null) {
      return map;
    }
    for (ExportCategory exp : exportCategories) {
      CategoryEntity entity = categoryRepository.findByUuid(exp.getUuid()).orElseGet(CategoryEntity::new);
      entity.setUuid(exp.getUuid());
      entity.setName(exp.getName());
      entity.setDateAdded(exp.getDateAdded() != null ? exp.getDateAdded() : LocalDateTime.now());
      entity.setDateModified(exp.getDateModified() != null ? exp.getDateModified() : LocalDateTime.now());
      map.put(exp.getUuid(), categoryRepository.save(entity));
    }
    return map;
  }

  private Map<UUID, TagEntity> importTags(List<ExportTag> exportTags) {
    Map<UUID, TagEntity> map = new HashMap<>();
    if (exportTags == null) {
      return map;
    }
    for (ExportTag exp : exportTags) {
      TagEntity entity = tagRepository.findByUuid(exp.getUuid()).orElseGet(TagEntity::new);
      entity.setUuid(exp.getUuid());
      entity.setName(exp.getName());
      entity.setDateAdded(exp.getDateAdded() != null ? exp.getDateAdded() : LocalDateTime.now());
      entity.setDateModified(exp.getDateModified() != null ? exp.getDateModified() : LocalDateTime.now());
      map.put(exp.getUuid(), tagRepository.save(entity));
    }
    return map;
  }

  private void importCategoryAttributeTemplates(List<ExportCategoryAttributeTemplate> templates,
                                                Map<UUID, CategoryEntity> categories) {
    if (templates == null) {
      return;
    }
    for (ExportCategoryAttributeTemplate exp : templates) {
      CategoryEntity cat = categories.get(exp.getCategory());
      if (cat == null) {
        continue;
      }
      CategoryAttributeTemplateEntity entity = new CategoryAttributeTemplateEntity();
      entity.setUuid(exp.getUuid());
      entity.setCategory(cat);
      entity.setAttributeName(exp.getAttributeName());
      entity.setDateAdded(exp.getDateAdded() != null ? exp.getDateAdded() : LocalDateTime.now());
      entity.setDateModified(exp.getDateModified() != null ? exp.getDateModified() : LocalDateTime.now());
      categoryAttributeTemplateRepository.save(entity);
    }
  }

  private Map<UUID, ItemEntity> importItems(List<ExportItem> exportItems,
                                            Map<UUID, CategoryEntity> categories,
                                            Map<UUID, StorageEntity> storages,
                                            Map<UUID, TagEntity> tags,
                                            Map<UUID, ImageEntity> images) {
    Map<UUID, ItemEntity> map = new HashMap<>();
    if (exportItems == null) {
      return map;
    }
    for (ExportItem exp : exportItems) {
      ItemEntity entity = itemRepository.findByUuid(exp.getUuid()).orElseGet(ItemEntity::new);
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

package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.mapper.ItemMapper;
import de.iske.kistogramm.model.*;
import de.iske.kistogramm.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final StorageRepository storageRepository;
    private final TagRepository tagRepository;
    private final ImageRepository imageRepository;
    private final CategoryAttributeTemplateRepository templateRepository;
    private final ItemMapper itemMapper;

    public ItemService(ItemRepository itemRepository,
                       CategoryRepository categoryRepository,
                       StorageRepository storageRepository,
                       TagRepository tagRepository,
                       ImageRepository imageRepository,
                       CategoryAttributeTemplateRepository templateRepository,
                       ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.storageRepository = storageRepository;
        this.tagRepository = tagRepository;
        this.imageRepository = imageRepository;
        this.templateRepository = templateRepository;
        this.itemMapper = itemMapper;
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll().stream().map(entity -> {
            Item dto = itemMapper.toDto(entity);
            dto.setTagIds(entity.getTags().stream().map(TagEntity::getId).collect(Collectors.toSet()));
            dto.setImageIds(entity.getImages().stream().map(ImageEntity::getId).collect(Collectors.toSet()));
            dto.setRelatedItemIds(entity.getRelatedItems().stream().map(ItemEntity::getId).collect(Collectors.toSet()));
            return dto;
        }).toList();
    }

    public Optional<Item> getItemById(Integer id) {
        return itemRepository.findById(id).map(entity -> {
            Item dto = itemMapper.toDto(entity);
            dto.setTagIds(entity.getTags().stream().map(TagEntity::getId).collect(Collectors.toSet()));
            dto.setImageIds(entity.getImages().stream().map(ImageEntity::getId).collect(Collectors.toSet()));
            dto.setRelatedItemIds(entity.getRelatedItems().stream().map(ItemEntity::getId).collect(Collectors.toSet()));
            return dto;
        });
    }

    public Item createItem(Item dto) {
        ItemEntity entity = itemMapper.toEntity(dto);

        // set category
        if (dto.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            entity.setCategory(category);

            List<CategoryAttributeTemplateEntity> templates =
                    templateRepository.findByCategoryId(dto.getCategoryId());

            if (dto.getDynamicAttributes() == null) {
                dto.setDynamicAttributes(new HashMap<>());
            }

            for (CategoryAttributeTemplateEntity template : templates) {
                dto.getDynamicAttributes().putIfAbsent(template.getAttributeName(), "");
            }
        }

        // set storage
        if (dto.getStorageId() != null) {
            StorageEntity storage = storageRepository.findById(dto.getStorageId())
                    .orElseThrow(() -> new IllegalArgumentException("Storage not found"));
            entity.setStorage(storage);
        }

        // set tags
        if (dto.getTagIds() != null) {
            Set<TagEntity> tags = new HashSet<>(tagRepository.findAllById(dto.getTagIds()));
            entity.setTags(tags);
        }

        // set related items
        if (dto.getRelatedItemIds() != null) {
            Set<ItemEntity> related = new HashSet<>(itemRepository.findAllById(dto.getRelatedItemIds()));
            entity.setRelatedItems(related);
        }

        // set images
        if (dto.getImageIds() != null) {
            Set<ImageEntity> images = new HashSet<>(imageRepository.findAllById(dto.getImageIds()));
            entity.setImages(images);
        }

        // dynamic attributes
        entity.setDynamicAttributes(dto.getDynamicAttributes());
        entity.setDateAdded(LocalDate.now());
        entity.setDateModified(LocalDate.now());

        return itemMapper.toDto(itemRepository.save(entity));
    }

    public void deleteItem(Integer id) {
        itemRepository.deleteById(id);
    }

    public Item updateItem(Integer id, Item updatedItem) {
        ItemEntity entity = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));

        // Update der Felder
        entity.setName(updatedItem.getName());
        entity.setDescription(updatedItem.getDescription());
        entity.setPurchaseDate(updatedItem.getPurchaseDate());
        entity.setPurchasePrice(updatedItem.getPurchasePrice());
        entity.setQuantity(updatedItem.getQuantity());

        // Kategorie ändern
        if (!Objects.equals(entity.getCategory().getId(), updatedItem.getCategoryId())) {
            CategoryEntity newCategory = categoryRepository.findById(updatedItem.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + updatedItem.getCategoryId()));
            entity.setCategory(newCategory);

            // Dynamische Felder neu initialisieren (nicht löschen!)
            Map<String, String> newAttrs = new HashMap<>(entity.getDynamicAttributes());

            List<CategoryAttributeTemplateEntity> templates = templateRepository.findByCategoryId(newCategory.getId());

            for (CategoryAttributeTemplateEntity template : templates) {
                newAttrs.putIfAbsent(template.getAttributeName(), "");
            }

            entity.setDynamicAttributes(newAttrs);
        }

        itemRepository.save(entity);
        return itemMapper.toDto(entity);
    }

    public Item linkRelatedItems(Integer itemId, List<Integer> relatedItemIds) {
        ItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        List<ItemEntity> relatedItems = itemRepository.findAllById(relatedItemIds);

        // Verknüpfung in beide Richtungen, falls bidirektional nötig
        for (ItemEntity related : relatedItems) {
            item.getRelatedItems().add(related);
            related.getRelatedItems().add(item);
        }

        item.setDateModified(LocalDate.now());

        itemRepository.saveAll(relatedItems);
        ItemEntity saved = itemRepository.save(item);

        return itemMapper.toDto(saved);
    }
}

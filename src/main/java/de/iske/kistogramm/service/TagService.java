package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.dto.Tag;
import de.iske.kistogramm.mapper.TagMapper;
import de.iske.kistogramm.model.TagEntity;
import de.iske.kistogramm.repository.ItemRepository;
import de.iske.kistogramm.repository.StorageRepository;
import de.iske.kistogramm.repository.TagRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final ItemRepository itemRepository;
    private final StorageRepository storageRepository;
    private final ItemService itemService;
    private final TagMapper tagMapper;

    public TagService(
            TagRepository tagRepository,
            ItemRepository itemRepository,
            StorageRepository storageRepository,
            TagMapper tagMapper,
            ItemService itemService) {
        this.tagRepository = tagRepository;
        this.itemRepository = itemRepository;
        this.storageRepository = storageRepository;
        this.itemService = itemService;
        this.tagMapper = tagMapper;
    }

    public List<Tag> getAllTags() {
        return tagRepository.findAll().stream()
                .map(tagMapper::toDto)
                .toList();
    }

    public Optional<Tag> getTagById(Integer id) {
        return tagRepository.findById(id)
                .map(tagMapper::toDto);
    }

    public Tag createTag(Tag tag) {
        TagEntity entity = tagMapper.toEntity(tag);
        entity.setDateAdded(LocalDateTime.now());
        entity.setDateModified(LocalDateTime.now());
        return tagMapper.toDto(tagRepository.save(entity));
    }

    public Tag updateTag(Tag tag) {
        TagEntity entity = tagRepository.findById(tag.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tag not found: " + tag.getId()));

        entity.setName(tag.getName());
        entity.setDateModified(LocalDateTime.now());

        TagEntity saved = tagRepository.save(entity);
        return tagMapper.toDto(saved);
    }

    public void deleteTag(Integer id) {
        long itemCount = itemRepository.findByTagsId(id).size();
        long storageCount = storageRepository.findByTagsId(id).size();
        if (itemCount > 0 || storageCount > 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Tag wird noch verwendet (" + itemCount + " Gegenstände, " + storageCount + " Lagerorte)"
            );
        }
        tagRepository.deleteById(id);
    }

    public List<Item> getItemsByTagId(Integer tagId) {
        return itemService.getItemsByTagId(tagId);
    }
}

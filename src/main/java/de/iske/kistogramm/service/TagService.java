package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.Tag;
import de.iske.kistogramm.mapper.TagMapper;
import de.iske.kistogramm.model.TagEntity;
import de.iske.kistogramm.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public TagService(TagRepository tagRepository, TagMapper tagMapper) {
        this.tagRepository = tagRepository;
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

    public void deleteTag(Integer id) {
        tagRepository.deleteById(id);
    }
}

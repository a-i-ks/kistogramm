package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.Storage;
import de.iske.kistogramm.mapper.StorageMapper;
import de.iske.kistogramm.model.RoomEntity;
import de.iske.kistogramm.model.StorageEntity;
import de.iske.kistogramm.model.TagEntity;
import de.iske.kistogramm.repository.RoomRepository;
import de.iske.kistogramm.repository.StorageRepository;
import de.iske.kistogramm.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StorageService {

    private final StorageRepository storageRepository;
    private final RoomRepository roomRepository;
    private final TagRepository tagRepository;
    private final StorageMapper storageMapper;

    public StorageService(StorageRepository storageRepository,
                          RoomRepository roomRepository,
                          TagRepository tagRepository,
                          StorageMapper storageMapper) {
        this.storageRepository = storageRepository;
        this.roomRepository = roomRepository;
        this.tagRepository = tagRepository;
        this.storageMapper = storageMapper;
    }

    public List<Storage> getAll() {
        return storageRepository.findAll().stream()
                .map(entity -> {
                    Storage dto = storageMapper.toDto(entity);
                    List<Integer> tagIds = entity.getTags() != null
                            ? entity.getTags().stream().map(TagEntity::getId).collect(Collectors.toList())
                            : new ArrayList<>();
                    dto.setTagIds(tagIds);
                    return dto;
                })
                .toList();
    }

    public Optional<Storage> getById(Integer id) {
        return storageRepository.findById(id)
                .map(entity -> {
                    Storage dto = storageMapper.toDto(entity);
                    List<Integer> tagIds = entity.getTags() != null
                            ? entity.getTags().stream().map(TagEntity::getId).collect(Collectors.toList())
                            : new ArrayList<>();
                    dto.setTagIds(tagIds);
                    return dto;
                });
    }

    @Transactional
    public Storage create(Storage dto) {
        StorageEntity entity = storageMapper.toEntity(dto);

        // set room
        if (dto.getRoomId() != null) {
            RoomEntity room = roomRepository.findById(dto.getRoomId())
                    .orElseThrow(() -> new IllegalArgumentException("Room not found"));
            entity.setRoom(room);
        }

        // set parent storage
        if (dto.getParentStorageId() != null) {
            StorageEntity parent = storageRepository.findById(dto.getParentStorageId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent storage not found"));
            entity.setParentStorage(parent);
        }

        // set tags
        if (dto.getTagIds() != null) {
            Set<TagEntity> tags = new HashSet<>(tagRepository.findAllById(dto.getTagIds()));
            entity.setTags(tags);
        }

        entity.setDateAdded(LocalDateTime.now());
        entity.setDateModified(LocalDateTime.now());

        return storageMapper.toDto(storageRepository.save(entity));
    }

    public void delete(Integer id) {
        storageRepository.deleteById(id);
    }
}

package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.Image;
import de.iske.kistogramm.dto.Storage;
import de.iske.kistogramm.mapper.ImageMapper;
import de.iske.kistogramm.mapper.StorageMapper;
import de.iske.kistogramm.model.ImageEntity;
import de.iske.kistogramm.model.RoomEntity;
import de.iske.kistogramm.model.StorageEntity;
import de.iske.kistogramm.model.TagEntity;
import de.iske.kistogramm.repository.ImageRepository;
import de.iske.kistogramm.repository.RoomRepository;
import de.iske.kistogramm.repository.StorageRepository;
import de.iske.kistogramm.repository.TagRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StorageService {

    private final StorageRepository storageRepository;
    private final RoomRepository roomRepository;
    private final TagRepository tagRepository;
    private final StorageMapper storageMapper;
    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;

    public StorageService(StorageRepository storageRepository,
                          RoomRepository roomRepository,
                          TagRepository tagRepository,
                          ImageRepository imageRepository,
                          StorageMapper storageMapper,
                          ImageMapper imageMapper) {
        this.storageRepository = storageRepository;
        this.roomRepository = roomRepository;
        this.imageRepository = imageRepository;
        this.tagRepository = tagRepository;
        this.storageMapper = storageMapper;
        this.imageMapper = imageMapper;
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
        } else {
            throw new IllegalArgumentException("roomId must be provided");
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

    public Storage updateStorage(Integer id, Storage updatedStorage) {
        StorageEntity entity = storageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Storage not found: " + id));

        entity.setName(updatedStorage.getName());
        entity.setDescription(updatedStorage.getDescription());
        entity.setDateModified(LocalDateTime.now());

        if (updatedStorage.getRoomId() != null) {
            RoomEntity room = roomRepository.findById(updatedStorage.getRoomId())
                    .orElseThrow(() -> new EntityNotFoundException("Room not found: " + updatedStorage.getRoomId()));
            entity.setRoom(room);
        }

        if (updatedStorage.getParentStorageId() != null) {
            StorageEntity parent = storageRepository.findById(updatedStorage.getParentStorageId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent storage not found: " + updatedStorage.getParentStorageId()));
            entity.setParentStorage(parent);
        } else {
            entity.setParentStorage(null);
        }

        // Optional: Tags neu zuweisen
        if (updatedStorage.getTagIds() != null) {
            List<TagEntity> tags = tagRepository.findAllById(updatedStorage.getTagIds());
            entity.setTags(new HashSet<>(tags));
        }

        StorageEntity saved = storageRepository.save(entity);
        return storageMapper.toDto(saved);
    }

    public void delete(Integer id) {
        storageRepository.deleteById(id);
    }

    public Storage uploadImages(Integer storageId, List<MultipartFile> files) {
        StorageEntity storage = storageRepository.findById(storageId)
                .orElseThrow(() -> new EntityNotFoundException("Storage not found: " + storageId));

        for (MultipartFile file : files) {
            try {
                ImageEntity image = new ImageEntity();
                image.setData(file.getBytes());
                image.setDateAdded(LocalDateTime.now());
                image.setDateModified(LocalDateTime.now());
                image.setStorage(storage);
                imageRepository.save(image);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read uploaded file", e);
            }
        }

        storage.setDateModified(LocalDateTime.now());

        return storageMapper.toDto(storageRepository.save(storage));
    }

    public List<Image> getImagesByStorageId(Integer storageId) {
        StorageEntity storage = storageRepository.findById(storageId)
                .orElseThrow(() -> new EntityNotFoundException("Storage not found: " + storageId));

        return storage.getImages()
                .stream().map(imageMapper::toDto)
                .collect(Collectors.toList());
    }

    public Image getImageByStorageIdAndImageId(Integer storageId, Integer imageId) {
        StorageEntity storage = storageRepository.findById(storageId)
                .orElseThrow(() -> new EntityNotFoundException("Storage not found: " + storageId));

        ImageEntity imageEntity = storage.getImages().stream()
                .filter(p -> p.getId().equals(imageId)).findFirst().orElseThrow(
                        () -> new EntityNotFoundException("Image not found: " + imageId));

        return imageMapper.toDto(imageEntity);
    }

    public void deleteImageFromStorage(Integer storageId, Integer imageId) {
        StorageEntity storage = storageRepository.findById(storageId)
                .orElseThrow(() -> new IllegalArgumentException("Storage not found: " + storageId));

        ImageEntity image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId));

        if (!storage.getImages().contains(image)) {
            throw new IllegalArgumentException("Image does not belong to this storage");
        }

        storage.setDateModified(LocalDateTime.now());

        storage.getImages().remove(image);
        imageRepository.delete(image);
        storageRepository.save(storage);
    }

    public void deleteAllImagesFromStorage(Integer storageId) {
        StorageEntity storage = storageRepository.findById(storageId)
                .orElseThrow(() -> new IllegalArgumentException("Storage not found: " + storageId));
        if (storage.getImages() != null) {
            for (ImageEntity image : storage.getImages()) {
                imageRepository.delete(image);
            }
            storage.getImages().clear();
            storage.setDateModified(LocalDateTime.now());
            storageRepository.save(storage);
        }
    }
}

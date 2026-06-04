package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.Image;
import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.dto.Room;
import de.iske.kistogramm.dto.Storage;
import de.iske.kistogramm.mapper.ImageMapper;
import de.iske.kistogramm.mapper.ItemMapper;
import de.iske.kistogramm.mapper.RoomMapper;
import de.iske.kistogramm.mapper.StorageMapper;
import de.iske.kistogramm.model.ImageEntity;
import de.iske.kistogramm.model.RoomEntity;
import de.iske.kistogramm.repository.ImageRepository;
import de.iske.kistogramm.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import de.iske.kistogramm.service.ImageCompressionService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;
    private final ImageRepository imageRepository;
    private final StorageMapper storageMapper;
    private final RoomMapper roomMapper;
    private final ImageMapper imageMapper;
    private final ItemMapper itemMapper;
    private final ImageCompressionService imageCompressionService;

    public RoomService(
            RoomRepository roomRepository,
            ImageRepository imageRepository,
            StorageMapper storageMapper,
            ImageMapper imageMapper,
            ItemMapper itemMapper,
            RoomMapper roomMapper,
            ImageCompressionService imageCompressionService) {
        this.roomRepository = roomRepository;
        this.imageRepository = imageRepository;
        this.storageMapper = storageMapper;
        this.roomMapper = roomMapper;
        this.itemMapper = itemMapper;
        this.imageMapper = imageMapper;
        this.imageCompressionService = imageCompressionService;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(roomMapper::toDto)
                .toList();
    }

    public Optional<Room> getRoomById(Integer id) {
        return roomRepository.findById(id)
                .map(roomMapper::toDto);
    }

    public Room createRoom(Room room) {
        RoomEntity entity = roomMapper.toEntity(room);
        entity.setDateAdded(LocalDateTime.now());
        entity.setDateModified(LocalDateTime.now());
        Room created = roomMapper.toDto(roomRepository.save(entity));
        log.info("Room created: id={} name='{}'", created.getId(), created.getName());
        return created;
    }

    public Room updateRoom(Integer id, Room updatedRoom) {
        RoomEntity entity = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + id));

        entity.setName(updatedRoom.getName());
        entity.setDescription(updatedRoom.getDescription());
        entity.setDateModified(LocalDateTime.now());

        RoomEntity saved = roomRepository.save(entity);
        return roomMapper.toDto(saved);
    }

    public void deleteRoom(Integer id) {
        log.info("Room deleted: id={}", id);
        roomRepository.deleteById(id);
    }

    public List<Item> getItemsByRoomId(Integer roomId) {
        return roomRepository.findById(roomId)
                .map(RoomEntity::getStorages)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"))
                .stream()
                .flatMap(storage -> storage.getItems().stream())
                .map(itemMapper::toDto)
                .toList();
    }

    public List<Storage> getStorageByRoomId(Integer roomId) {
        return roomRepository.findById(roomId)
                .map(RoomEntity::getStorages)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"))
                .stream()
                .map(storageMapper::toDto)
                .toList();
    }

    public Room uploadImage(Integer roomId, MultipartFile file) {
        RoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        try {
            ImageEntity image = new ImageEntity();
            image.setData(imageCompressionService.compress(file.getBytes(), file.getContentType()));
            image.setType(file.getContentType());
            image.setDateAdded(LocalDateTime.now());
            image.setDateModified(LocalDateTime.now());
            imageRepository.save(image);

            room.setImage(image);
            room.setDateModified(LocalDateTime.now());

            return roomMapper.toDto(roomRepository.save(room));
        } catch (IOException e) {
            log.error("Failed to read uploaded image for roomId={}: {}", roomId, e.getMessage());
            throw new RuntimeException("Error reading uploaded file", e);
        }
    }

    public void deleteImage(Integer roomId) {
        RoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        if (room.getImage() != null) {
            imageRepository.delete(room.getImage());
            room.setImage(null);
            room.setDateModified(LocalDateTime.now());
            roomRepository.save(room);
        } else {
            throw new IllegalArgumentException("No image to delete for room: " + roomId);
        }
    }

    public Optional<Image> getRoomImage(Integer roomId) {
        return roomRepository.findById(roomId)
                .map(RoomEntity::getImage)
                .map(imageMapper::toDto);
    }
}

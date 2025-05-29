package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.dto.Room;
import de.iske.kistogramm.dto.Storage;
import de.iske.kistogramm.mapper.ItemMapper;
import de.iske.kistogramm.mapper.RoomMapper;
import de.iske.kistogramm.mapper.StorageMapper;
import de.iske.kistogramm.model.RoomEntity;
import de.iske.kistogramm.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final StorageMapper storageMapper;
    private final RoomMapper roomMapper;
    private final ItemMapper itemMapper;

    public RoomService(
            RoomRepository roomRepository,
            StorageMapper storageMapper,
            ItemMapper itemMapper,
            RoomMapper roomMapper) {
        this.roomRepository = roomRepository;
        this.storageMapper = storageMapper;
        this.roomMapper = roomMapper;
        this.itemMapper = itemMapper;
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
        return roomMapper.toDto(roomRepository.save(entity));
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
}

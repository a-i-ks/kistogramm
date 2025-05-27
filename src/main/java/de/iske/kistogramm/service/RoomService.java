package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.Room;
import de.iske.kistogramm.mapper.RoomMapper;
import de.iske.kistogramm.model.RoomEntity;
import de.iske.kistogramm.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;

    public RoomService(RoomRepository roomRepository, RoomMapper roomMapper) {
        this.roomRepository = roomRepository;
        this.roomMapper = roomMapper;
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
        entity.setDateAdded(LocalDate.now());
        entity.setDateModified(LocalDate.now());
        return roomMapper.toDto(roomRepository.save(entity));
    }

    public void deleteRoom(Integer id) {
        roomRepository.deleteById(id);
    }
}

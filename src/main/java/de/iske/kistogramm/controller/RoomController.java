package de.iske.kistogramm.controller;

import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.dto.Room;
import de.iske.kistogramm.dto.Storage;
import de.iske.kistogramm.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        return roomService.getAllRooms();
    }

    @GetMapping("/{roomId}/items")
    @Transactional(readOnly = true)
    public List<Item> getItemsByRoomId(@PathVariable Integer roomId) {
        return roomService.getItemsByRoomId(roomId);
    }

    @GetMapping("/{roomId}/storage")
    @Transactional(readOnly = true)
    public List<Storage> getStorageByRoomId(@PathVariable Integer roomId) {
        return roomService.getStorageByRoomId(roomId);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Room> getRoomById(@PathVariable Integer id) {
        return roomService.getRoomById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        return ResponseEntity.ok(roomService.createRoom(room));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(@PathVariable Integer id, @RequestBody Room updatedRoom) {
        return ResponseEntity.ok(roomService.updateRoom(id, updatedRoom));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Integer id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}

package de.iske.kistogramm.controller;

import de.iske.kistogramm.dto.Image;
import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.dto.Room;
import de.iske.kistogramm.dto.Storage;
import de.iske.kistogramm.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @Transactional
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        return ResponseEntity.ok(roomService.createRoom(room));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Room> updateRoom(@PathVariable Integer id, @RequestBody Room updatedRoom) {
        return ResponseEntity.ok(roomService.updateRoom(id, updatedRoom));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteRoom(@PathVariable Integer id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/image")
    @Transactional(readOnly = true)
    public ResponseEntity<Image> getRoomImage(@PathVariable Integer roomId) {
        return roomService.getRoomImage(roomId)
                .map(imageData -> ResponseEntity.ok().body(imageData))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{roomId}/image")
    @Transactional
    public ResponseEntity<Room> uploadRoomImage(
            @PathVariable Integer roomId,
            @RequestParam("file") MultipartFile file) {
        Room updated = roomService.uploadImage(roomId, file);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{roomId}/image")
    @Transactional
    public ResponseEntity<Void> deleteRoomImage(@PathVariable Integer roomId) {
        roomService.deleteImage(roomId);
        return ResponseEntity.noContent().build();
    }
}

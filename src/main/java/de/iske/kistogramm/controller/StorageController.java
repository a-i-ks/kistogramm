package de.iske.kistogramm.controller;

import de.iske.kistogramm.dto.Storage;
import de.iske.kistogramm.service.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/storages")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping
    public List<Storage> getAllStorages() {
        return storageService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Storage> getStorageById(@PathVariable Integer id) {
        return storageService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Storage> createStorage(@RequestBody Storage storage) {
        return ResponseEntity.ok(storageService.create(storage));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStorage(@PathVariable Integer id) {
        storageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

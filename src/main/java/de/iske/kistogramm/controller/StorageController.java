package de.iske.kistogramm.controller;

import de.iske.kistogramm.dto.Image;
import de.iske.kistogramm.dto.Storage;
import de.iske.kistogramm.service.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/storages")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Storage> getAllStorages() {
        return storageService.getAll();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Storage> getStorageById(@PathVariable Integer id) {
        return storageService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Storage> createStorage(@RequestBody Storage storage) {
        return ResponseEntity.ok(storageService.create(storage));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Storage> updateStorage(@PathVariable Integer id, @RequestBody Storage storage) {
        Storage updated = storageService.updateStorage(id, storage);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteStorage(@PathVariable Integer id) {
        storageService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<Storage> uploadImages(@PathVariable Integer id,
                                                @RequestParam("files") List<MultipartFile> files) {
        var updatedStorage = storageService.uploadImages(id, files);
        return ResponseEntity.ok(updatedStorage);
    }

    @GetMapping("/{id}/images")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Image>> getImagesByStorageId(@PathVariable Integer id) {
        List<Image> images = storageService.getImagesByStorageId(id);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/{storageId}/images/{imageId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Image> getImageByStorageIdAndImageId(@PathVariable Integer storageId, @PathVariable Integer imageId) {
        return ResponseEntity.ok(storageService.getImageByStorageIdAndImageId(storageId, imageId));
    }

    @DeleteMapping("/{storageId}/images/{imageId}")
    @Transactional
    public ResponseEntity<Void> deleteImageFromStorage(@PathVariable Integer storageId, @PathVariable Integer imageId) {
        storageService.deleteImageFromStorage(storageId, imageId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{storageId}/images")
    @Transactional
    public ResponseEntity<Void> deleteAllImagesFromStorage(@PathVariable Integer storageId) {
        storageService.deleteAllImagesFromStorage(storageId);
        return ResponseEntity.ok().build();
    }

}

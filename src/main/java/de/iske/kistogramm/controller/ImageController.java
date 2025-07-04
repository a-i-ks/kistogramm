package de.iske.kistogramm.controller;

import de.iske.kistogramm.dto.Image;
import de.iske.kistogramm.service.ImageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Image> getAllImages() {
        return imageService.getAllImages();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Image> getImageById(@PathVariable Integer id) {
        return imageService.getImageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/data")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getImageData(@PathVariable Integer id) {
        return imageService.getImageById(id)
                .map(image -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType(image.getType()));
                    return new ResponseEntity<>(image.getData(), headers, HttpStatus.OK);
                }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Image> createImage(@RequestBody Image image) {
        return ResponseEntity.ok(imageService.createImage(image));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteImage(@PathVariable Integer id) {
        imageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload")
    @Transactional
    public ResponseEntity<Image> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        Image image = new Image();
        image.setData(file.getBytes());
        image.setType(file.getContentType());
        image.setDateAdded(LocalDateTime.now());
        image.setDateModified(LocalDateTime.now());
        return ResponseEntity.ok(imageService.createImage(image));
    }
}

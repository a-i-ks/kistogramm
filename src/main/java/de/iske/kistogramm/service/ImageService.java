package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.Image;
import de.iske.kistogramm.mapper.ImageMapper;
import de.iske.kistogramm.model.ImageEntity;
import de.iske.kistogramm.repository.ImageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;

    public ImageService(ImageRepository imageRepository, ImageMapper imageMapper) {
        this.imageRepository = imageRepository;
        this.imageMapper = imageMapper;
    }

    public List<Image> getAllImages() {
        return imageRepository.findAll().stream()
                .map(imageMapper::toDto)
                .toList();
    }

    public Optional<Image> getImageById(Integer id) {
        return imageRepository.findById(id)
                .map(imageMapper::toDto);
    }

    public Image createImage(Image image) {
        ImageEntity entity = imageMapper.toEntity(image);
        entity.setDateAdded(LocalDateTime.now());
        entity.setDateModified(LocalDateTime.now());
        return imageMapper.toDto(imageRepository.save(entity));
    }

    public void deleteImage(Integer id) {
        imageRepository.deleteById(id);
    }
}

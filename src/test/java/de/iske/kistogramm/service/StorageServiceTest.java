package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.Storage;
import de.iske.kistogramm.model.RoomEntity;
import de.iske.kistogramm.repository.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class StorageServiceTest {

    @Autowired
    private StorageService storageService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private StorageRepository storageRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private CategoryAttributeTemplateRepository categoryAttributeTemplateRepository;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    void setUp() {
        // Cleanup before each test to ensure a clean state
        // Unlink all images from items to avoid foreign key constraint issues
        itemRepository.findAll().forEach(item -> {
            item.setImages(null);
            itemRepository.save(item);
        });
        // Unlink all images from storages to avoid foreign key constraint issues
        storageRepository.findAll().forEach(storage -> {
            storage.setImages(null);
            storageRepository.save(storage);
        });
        // Unlink all images from rooms to avoid foreign key constraint issues
        roomRepository.findAll().forEach(room -> {
            room.setImage(null);
            roomRepository.save(room);
        });

        // Clear all repositories before each test to ensure a clean state
        imageRepository.deleteAll();
        itemRepository.deleteAll();
        storageRepository.deleteAll();
        categoryAttributeTemplateRepository.deleteAll();
        categoryRepository.deleteAll();
        tagRepository.deleteAll();
        roomRepository.deleteAll();
    }

    @Test
    void testCreateAndFindStorage() {
        RoomEntity room = new RoomEntity();
        room.setName("Test Room");
        room.setDateAdded(LocalDateTime.now());
        room.setDateModified(LocalDateTime.now());
        room = roomRepository.save(room);

        Storage dto = new Storage();
        dto.setName("Box A");
        dto.setDescription("Test Box");
        dto.setRoomId(room.getId());

        Storage created = storageService.create(dto);
        assertNotNull(created.getId());

        Optional<Storage> loaded = storageService.getById(created.getId());
        assertTrue(loaded.isPresent());
        assertEquals("Box A", loaded.get().getName());
    }
}

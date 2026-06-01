package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.Storage;
import de.iske.kistogramm.model.RoomEntity;
import de.iske.kistogramm.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
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

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        itemRepository.findAll().forEach(item -> {
            item.setImages(null);
            itemRepository.save(item);
        });
        storageRepository.findAll().forEach(storage -> {
            storage.setImages(null);
            storageRepository.save(storage);
        });
        roomRepository.findAll().forEach(room -> {
            room.setImage(null);
            roomRepository.save(room);
        });

        imageRepository.deleteAll();
        itemRepository.deleteAll();
        storageRepository.deleteAll();
        categoryAttributeTemplateRepository.deleteAll();
        categoryRepository.deleteAll();
        tagRepository.deleteAll();
        roomRepository.deleteAll();
        // Force Hibernate to flush DELETEs before any subsequent INSERTs
        entityManager.flush();
        entityManager.clear();
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

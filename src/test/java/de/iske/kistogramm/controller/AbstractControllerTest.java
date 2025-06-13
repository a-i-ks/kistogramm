package de.iske.kistogramm.controller;

import de.iske.kistogramm.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
public abstract class AbstractControllerTest {

    @Autowired
    protected CategoryRepository categoryRepository;
    @Autowired
    protected CategoryAttributeTemplateRepository categoryAttributeTemplateRepository;
    @Autowired
    protected ItemRepository itemRepository;
    @Autowired
    protected ImageRepository imageRepository;
    @Autowired
    protected StorageRepository storageRepository;
    @Autowired
    protected RoomRepository roomRepository;
    @Autowired
    protected TagRepository tagRepository;

    @BeforeEach
    void cleanDatabase() {
        // unlink images to avoid foreign key constraints
        itemRepository.findAll().forEach(item -> {
            item.setImages(null);
            item.setReceipts(null);
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
    }
}

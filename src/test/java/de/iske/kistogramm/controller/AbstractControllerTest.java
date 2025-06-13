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
    protected CategoryAttributeTemplateRepository templateRepository;
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
        // unlink room images to avoid foreign key constraints
        roomRepository.findAll().forEach(room -> {
            room.setImage(null);
            roomRepository.save(room);
        });

        imageRepository.deleteAll();
        itemRepository.deleteAll();
        storageRepository.deleteAll();
        templateRepository.deleteAll();
        categoryRepository.deleteAll();
        tagRepository.deleteAll();
        roomRepository.deleteAll();
    }
}

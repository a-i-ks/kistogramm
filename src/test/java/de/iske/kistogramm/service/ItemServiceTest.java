package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.model.CategoryEntity;
import de.iske.kistogramm.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class ItemServiceTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private CategoryRepository categoryRepository;

    private Integer testCategoryId;

    @BeforeEach
    void setup() {
        CategoryEntity category = new CategoryEntity();
        category.setName("TestCategory");
        category.setDateAdded(java.time.LocalDateTime.now());
        category.setDateModified(java.time.LocalDateTime.now());
        testCategoryId = categoryRepository.save(category).getId();
    }

    @Test
    void testCreateAndRetrieveItem() {
        Item item = new Item();
        item.setName("Test Item");
        item.setDescription("Desc");
        item.setCategoryId(testCategoryId);
        item.setQuantity(1);
        item.setCustomAttributes(new HashMap<>());

        Item created = itemService.createItem(item);

        assertNotNull(created.getId());
        Optional<Item> retrieved = itemService.getItemById(created.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("Test Item", retrieved.get().getName());
    }
}

package de.iske.kistogramm.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.Category;
import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldCreateCategorySuccessfully() throws Exception {
        // Arrange
        Category category = new Category();
        category.setName("Werkzeug");

        // Act
        String response = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(category)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Category savedCategory = objectMapper.readValue(response, Category.class);

        // Assert
        assertThat(savedCategory.getId()).isNotNull();
        assertThat(savedCategory.getName()).isEqualTo("Werkzeug");
        assertThat(savedCategory.getDateAdded()).isNotNull();
        assertThat(savedCategory.getDateModified()).isNotNull();
    }

    @Test
    void shouldUpdateCategoryNameSuccessfully() throws Exception {
        // Step 1: Ursprüngliche Kategorie anlegen
        Category original = new Category();
        original.setName("Cat1");

        String json = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(original)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Category created = objectMapper.readValue(json, Category.class);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Cat1");

        // Step 2: Kategorie ändern
        created.setName("Haushaltsgeräte");

        String updatedJson = mockMvc.perform(put("/api/categories/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Category updated = objectMapper.readValue(updatedJson, Category.class);

        // Step 3: Validierung
        assertThat(updated.getName()).isEqualTo("Haushaltsgeräte");

        // Optional: nochmal abrufen und prüfen
        String response = mockMvc.perform(get("/api/categories/" + created.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Category fetched = objectMapper.readValue(response, Category.class);
        assertThat(fetched.getName()).isEqualTo("Haushaltsgeräte");
    }

    @Test
    void shouldCreateMultipleCategoriesAndFetchAll() throws Exception {
        // Arrange: Kategorien erstellen
        List<String> names = List.of("K1", "K2", "K3");
        for (String name : names) {
            Category category = new Category();
            category.setName(name);

            mockMvc.perform(post("/api/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(category)))
                    .andExpect(status().isOk());
        }

        // Act: Alle Kategorien abrufen
        String response = mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Category> categories = objectMapper.readValue(response,
                new TypeReference<List<Category>>() {
                });

        // Assert: Erwartete Namen enthalten
        List<String> returnedNames = categories.stream().map(Category::getName).toList();
        assertThat(returnedNames).containsAll(names);
    }

    @Test
    void shouldDeleteOneCategoryAndKeepTheOther() throws Exception {
        // Step 1: Zwei Kategorien anlegen
        Category cat1 = new Category();
        cat1.setName("Pflanzen");

        Category cat2 = new Category();
        cat2.setName("Werkzeuge");

        Category savedCat1 = objectMapper.readValue(
                mockMvc.perform(post("/api/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(cat1)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Category.class
        );

        Category savedCat2 = objectMapper.readValue(
                mockMvc.perform(post("/api/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(cat2)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Category.class
        );

        // Step 2: Eine Kategorie löschen
        mockMvc.perform(delete("/api/categories/" + savedCat1.getId()))
                .andExpect(status().isNoContent());

        // Step 3: Alle Kategorien abrufen
        String response = mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Category> remainingCategories = objectMapper.readValue(response,
                new TypeReference<>() {
                });

        // Step 4: Verifikation
        List<Integer> remainingIds = remainingCategories.stream()
                .map(Category::getId)
                .toList();

        assertThat(remainingIds).doesNotContain(savedCat1.getId());
        assertThat(remainingIds).contains(savedCat2.getId());
    }

    @Test
    void shouldOnlyDeleteCategoryWhenNotAssignedToAnyItem() throws Exception {
        // Step 1: Kategorie anlegen
        Category category = new Category();
        category.setName("Bücher");

        Category savedCategory = objectMapper.readValue(
                mockMvc.perform(post("/api/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(category)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Category.class
        );

        // Step 2: Item mit dieser Kategorie anlegen
        Item item = new Item();
        item.setName("Roman");
        item.setCategoryId(savedCategory.getId());

        Item savedItem = objectMapper.readValue(
                mockMvc.perform(post("/api/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(item)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Item.class
        );

        // Step 3: Versuch, die Kategorie zu löschen (soll fehlschlagen)
        mockMvc.perform(delete("/api/categories/" + savedCategory.getId()))
                .andExpect(status().isBadRequest());

        // Step 4: Kategorie vom Item entfernen
        savedItem.setCategoryId(null);
        mockMvc.perform(put("/api/items/" + savedItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(savedItem)))
                .andExpect(status().isOk());

        // Step 5: Kategorie jetzt erfolgreich löschen
        mockMvc.perform(delete("/api/categories/" + savedCategory.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnOnlyItemsForRequestedCategory() throws Exception {
        // Step 1: Zwei Kategorien anlegen
        Category catA = new Category();
        catA.setName("Drucker");

        Category catB = new Category();
        catB.setName("Scanner");

        Category savedCatA = objectMapper.readValue(
                mockMvc.perform(post("/api/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(catA)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Category.class
        );

        Category savedCatB = objectMapper.readValue(
                mockMvc.perform(post("/api/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(catB)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Category.class
        );

        // Step 2: Drei Items anlegen – zwei mit catA, eines mit catB
        List<Item> items = List.of(
                newItem("HP LaserJet", savedCatA.getId()),
                newItem("Canon Pixma", savedCatA.getId()),
                newItem("Epson Scan", savedCatB.getId())
        );

        for (Item item : items) {
            mockMvc.perform(post("/api/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(item)))
                    .andExpect(status().isOk());
        }

        // Step 3: Alle Items von Kategorie A abrufen
        String response = mockMvc.perform(get("/api/categories/" + savedCatA.getId() + "/items"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Item> result = objectMapper.readValue(response, new TypeReference<>() {
        });

        // Step 4: Validierung
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Item::getCategoryId)
                .containsOnly(savedCatA.getId());
        assertThat(result).extracting(Item::getName)
                .containsExactlyInAnyOrder("HP LaserJet", "Canon Pixma");
    }

    private Item newItem(String name, Integer categoryId) {
        Item item = new Item();
        item.setName(name);
        item.setCategoryId(categoryId);
        return item;
    }
}

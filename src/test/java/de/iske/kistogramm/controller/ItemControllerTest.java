package de.iske.kistogramm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.repository.CategoryRepository;
import de.iske.kistogramm.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ItemRepository itemRepository;


    private Integer clothingCategoryId;
    private Integer foodCategoryId;
    private Integer electronicCategoryId;


    @BeforeEach
    void setup() {
        clothingCategoryId = categoryRepository.findByName("Kleidung")
                .orElseThrow().getId();
        foodCategoryId = categoryRepository.findByName("Lebensmittel")
                .orElseThrow().getId();
        electronicCategoryId = categoryRepository.findByName("Elektronik")
                .orElseThrow().getId();
    }

    @Test
    void shouldCreateNewItemWithCorrectAddedAndModifiedDate() throws Exception {
        Item item = new Item();
        item.setName("Shirt");
        item.setCategoryId(clothingCategoryId);
        item.setQuantity(1);

        String response = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Item created = objectMapper.readValue(response, Item.class);
        assertThat(created.getDateAdded()).isBetween(LocalDateTime.now().minusSeconds(5), LocalDateTime.now());
        assertThat(created.getDateModified()).isBetween(LocalDateTime.now().minusSeconds(5), LocalDateTime.now());

    }

    @Test
    void shouldUpdateItemAndSetModifiedDate() throws Exception {
        Item item = new Item();
        item.setName("Hose");
        item.setCategoryId(clothingCategoryId);
        item.setQuantity(1);

        Item saved = objectMapper.readValue(
                mockMvc.perform(post("/api/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(item)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Item.class
        );

        saved.setName("Jeans");
        String updatedResponse = mockMvc.perform(put("/api/items/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saved)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Item updated = objectMapper.readValue(updatedResponse, Item.class);
        assertThat(updated.getDateModified()).isBetween(LocalDateTime.now().minusSeconds(5), LocalDateTime.now());
        assertThat(updated.getDateAdded()).isEqualToIgnoringNanos(saved.getDateAdded());
    }

    @Test
    void shouldLinkTwoItemsAndVerifyBothSides() throws Exception {
        // Schritt 1: Zwei Items anlegen
        Item item1 = new Item();
        item1.setName("Beamer");
        item1.setCategoryId(electronicCategoryId);
        item1.setQuantity(1);

        Item item2 = new Item();
        item2.setName("HDMI-Kabel");
        item2.setCategoryId(electronicCategoryId);
        item2.setQuantity(1);

        Item saved1 = objectMapper.readValue(mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item1)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        Item saved2 = objectMapper.readValue(mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item2)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        // Schritt 2: In Relation setzen
        mockMvc.perform(put("/api/items/" + saved1.getId() + "/related")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(saved2.getId()))))
                .andExpect(status().isOk());

        // Schritt 3: Reload Items
        Item reloaded1 = objectMapper.readValue(mockMvc.perform(get("/api/items/" + saved1.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        Item reloaded2 = objectMapper.readValue(mockMvc.perform(get("/api/items/" + saved2.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        // Schritt 4: Beide referenzieren einander
        assertThat(reloaded1.getRelatedItemIds()).contains(saved2.getId());
        assertThat(reloaded2.getRelatedItemIds()).contains(saved1.getId());

        // Schritt 5: Änderungsdatum gesetzt (heute)
        assertThat(reloaded1.getDateModified()).isBetween(LocalDateTime.now().minusSeconds(5), LocalDateTime.now());
        assertThat(reloaded2.getDateModified()).isBetween(LocalDateTime.now().minusSeconds(5), LocalDateTime.now());
    }


    @Test
    void whenCreatingClothingItemThenDynamicAttributesShouldContainCustomFields() throws Exception {
        Item item = new Item();
        item.setName("Jeans");
        item.setCategoryId(clothingCategoryId);
        item.setQuantity(1);
        item.setDynamicAttributes(new HashMap<>());

        String response = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Item created = objectMapper.readValue(response, Item.class);

        assertThat(created.getDynamicAttributes()).containsKeys("Größe", "Zuletzt getragen");
    }

    @Test
    void whenChangingCategoryThenNewAttributesShouldBeAddedAndOldShouldPersist() throws Exception {
        Item item = new Item();
        item.setName("Butter");
        item.setCategoryId(clothingCategoryId);
        item.setQuantity(1);
        item.setDynamicAttributes(Map.of("Größe", "M"));

        String createResp = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Item created = objectMapper.readValue(createResp, Item.class);
        created.setCategoryId(foodCategoryId); // change to "Lebensmittel"

        String updateResp = mockMvc.perform(put("/api/items/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Item updated = objectMapper.readValue(updateResp, Item.class);

        assertThat(updated.getDynamicAttributes())
                .containsKey("MHD")
                .containsKey("Größe");
    }

    @Test
    void whenAttributeAlreadyExistsThenDoNotDuplicateOnCategorySet() throws Exception {
        Item item = new Item();
        item.setName("T-Shirt");
        item.setCategoryId(clothingCategoryId);
        item.setQuantity(1);

        // Manuell vorher gesetztes Attribut
        Map<String, String> attributes = new HashMap<>();
        attributes.put("Größe", "L");
        item.setDynamicAttributes(attributes);

        String response = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Item created = objectMapper.readValue(response, Item.class);

        assertThat(created.getDynamicAttributes()).containsEntry("Größe", "L");
        assertThat(Collections.frequency(new ArrayList<>(created.getDynamicAttributes().keySet()), "Größe")).isEqualTo(1);
    }

    @Test
    void testCreateItemReturnsBadRequestWithoutName() throws Exception {
        Item item = new Item(); // name is required
        item.setQuantity(1);
        item.setDynamicAttributes(new HashMap<>());

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isBadRequest());
    }
}

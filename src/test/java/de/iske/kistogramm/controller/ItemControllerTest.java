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

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private Integer elektronikCategoryId;


    @BeforeEach
    void setup() {
        clothingCategoryId = categoryRepository.findByName("Kleidung")
                .orElseThrow().getId();
        foodCategoryId = categoryRepository.findByName("Lebensmittel")
                .orElseThrow().getId();
        elektronikCategoryId = categoryRepository.findByName("Elektronik")
                .orElseThrow().getId();
    }

    @Test
    void shouldCreateNewItemWithCorrectAddedDate() throws Exception {
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
        assertThat(created.getDateAdded()).isEqualTo(LocalDate.now());
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
        LocalDate dateAdded = saved.getDateAdded();

        saved.setName("Jeans");
        String updatedResponse = mockMvc.perform(put("/api/items/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saved)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Item updated = objectMapper.readValue(updatedResponse, Item.class);
        assertThat(updated.getDateModified()).isEqualTo(LocalDate.now());
        assertThat(updated.getDateAdded()).isEqualTo(saved.getDateAdded());
    }

    @Test
    void shouldLinkTwoItems() throws Exception {
        Item item1 = new Item();
        item1.setName("Monitor");
        item1.setCategoryId(elektronikCategoryId);
        item1.setQuantity(1);

        Item item2 = new Item();
        item2.setName("HDMI-Kabel");
        item2.setCategoryId(elektronikCategoryId);
        item2.setQuantity(1);

        Item saved1 = objectMapper.readValue(mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item1)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), Item.class);

        Item saved2 = objectMapper.readValue(mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item2)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), Item.class);

        Item connected = objectMapper.readValue(mockMvc.perform(put("/api/items/" + saved1.getId() + "/related")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(saved2.getId()))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), Item.class);

        assertThat(connected.getRelatedItemIds()).containsExactly(saved2.getId());
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

package de.iske.kistogramm.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.*;
import de.iske.kistogramm.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class SearchControllerTest {

    private UUID itemUuid;
    private UUID storageUuid;
    private UUID roomUuid;
    private UUID tagUuid;
    private UUID categoryUuid;


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private RoomRepository roomRepository;

    @BeforeEach
    void setUp() throws Exception {
        // Step 1: Create category
        Category category = new Category();
        category.setName("SearchTestCategory");

        Category savedCategory = objectMapper.readValue(mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(category)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Category.class);

        categoryUuid = savedCategory.getUuid();

        // Step 2: Create room
        Room room = new Room();
        room.setName("SearchTestRoom");
        room.setDescription("Room used in UUID search");

        Room savedRoom = objectMapper.readValue(mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Room.class);
        roomUuid = savedRoom.getUuid();

        // Step 3: Create storage
        Storage storage = new Storage();
        storage.setName("SearchTestStorage");
        storage.setDescription("Storage used in UUID search");
        storage.setRoomId(savedRoom.getId());

        Storage savedStorage = objectMapper.readValue(mockMvc.perform(post("/api/storages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storage)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Storage.class);
        storageUuid = savedStorage.getUuid();

        // Step 4: Create item
        Item item = new Item();
        item.setName("SearchableItem");
        item.setCategoryId(savedCategory.getId());
        item.setStorageId(savedStorage.getId());

        Item savedItem = objectMapper.readValue(mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);
        itemUuid = savedItem.getUuid();

        // Step 5: Create tag
        Tag tag = new Tag();
        tag.setName("SearchTestTag");

        Tag savedTag = objectMapper.readValue(mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tag)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Tag.class);
        tagUuid = savedTag.getUuid();
    }

    @Test
    void shouldFindItemByUuid() throws Exception {
        mockMvc.perform(get("/api/search/" + itemUuid))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String json = result.getResponse().getContentAsString();
                    assertThat(json).contains("SearchableItem");
                });
    }

    @Test
    void shouldFindStorageByUuid() throws Exception {
        mockMvc.perform(get("/api/search/" + storageUuid))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String json = result.getResponse().getContentAsString();
                    assertThat(json).contains("SearchTestStorage");
                });
    }

    @Test
    void shouldFindRoomByUuid() throws Exception {
        mockMvc.perform(get("/api/search/" + roomUuid))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String json = result.getResponse().getContentAsString();
                    assertThat(json).contains("SearchTestRoom");
                });
    }

    @Test
    void shouldFindTagByUuid() throws Exception {
        mockMvc.perform(get("/api/search/" + tagUuid))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String json = result.getResponse().getContentAsString();
                    assertThat(json).contains("SearchTestTag");
                });
    }

    @Test
    void shouldFindCategoryByUuid() throws Exception {
        mockMvc.perform(get("/api/search/" + categoryUuid))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String json = result.getResponse().getContentAsString();
                    assertThat(json).contains("SearchTestCategory");
                });
    }

    @Test
    void shouldReturnNotFoundForUnknownUuid() throws Exception {
        UUID unknown = UUID.randomUUID();

        mockMvc.perform(get("/api/search/" + unknown))
                .andExpect(status().isNotFound());
    }
}

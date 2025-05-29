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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
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

    private Integer createTag(String name) throws Exception {
        Map<String, String> tag = Map.of("name", name);
        String response = mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tag)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asInt();
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
    void shouldUnlinkOneItemAndKeepOthers() throws Exception {
        // A: Zentrales Item
        Item itemA = new Item();
        itemA.setName("Ladegerät");
        itemA.setCategoryId(electronicCategoryId);
        itemA.setQuantity(1);

        // B & C: Verbundene Items
        Item itemB = new Item();
        itemB.setName("USB-Kabel");

        Item itemC = new Item();
        itemC.setName("Powerbank");

        itemB.setCategoryId(electronicCategoryId);
        itemC.setCategoryId(electronicCategoryId);
        itemB.setQuantity(1);
        itemC.setQuantity(1);

        Item savedA = objectMapper.readValue(mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        Item savedB = objectMapper.readValue(mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemB)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        Item savedC = objectMapper.readValue(mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemC)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        // A mit B & C verlinken
        mockMvc.perform(put("/api/items/" + savedA.getId() + "/related")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(savedB.getId(), savedC.getId()))))
                .andExpect(status().isOk());

        // Relation zu B entfernen -> Neue Liste enthält nur C
        mockMvc.perform(put("/api/items/" + savedA.getId() + "/related")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(savedC.getId()))))
                .andExpect(status().isOk());

        // Reload aller Items
        Item reloadedA = objectMapper.readValue(mockMvc.perform(get("/api/items/" + savedA.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        Item reloadedB = objectMapper.readValue(mockMvc.perform(get("/api/items/" + savedB.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        Item reloadedC = objectMapper.readValue(mockMvc.perform(get("/api/items/" + savedC.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        // Verifikation
        assertThat(reloadedA.getRelatedItemIds()).containsExactlyInAnyOrder(savedC.getId());
        assertThat(reloadedB.getRelatedItemIds()).doesNotContain(savedA.getId());
        assertThat(reloadedC.getRelatedItemIds()).contains(savedA.getId());
    }

    @Test
    void shouldAssignSingleTagToItem() throws Exception {
        // Create item
        Item item = new Item();
        item.setName("Buch");

        Item savedItem = objectMapper.readValue(mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        // Create tag
        String tagName = "Wissen";
        Integer tagId = createTag(tagName);

        // Assign tag to item
        mockMvc.perform(put("/api/items/" + savedItem.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(tagId))))
                .andExpect(status().isOk());

        // Verify
        Item updatedItem = objectMapper.readValue(mockMvc.perform(get("/api/items/" + savedItem.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        assertThat(updatedItem.getTagIds()).containsExactly(tagId);
    }

    @Test
    void shouldAssignMultipleTagsToItem() throws Exception {
        Item item = new Item();
        item.setName("Laptop");

        Item savedItem = objectMapper.readValue(mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        Integer tagId1 = createTag("Technik");
        Integer tagId2 = createTag("Arbeit");

        mockMvc.perform(put("/api/items/" + savedItem.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(tagId1, tagId2))))
                .andExpect(status().isOk());

        Item updatedItem = objectMapper.readValue(mockMvc.perform(get("/api/items/" + savedItem.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        assertThat(updatedItem.getTagIds()).containsExactlyInAnyOrder(tagId1, tagId2);
    }

    @Test
    void shouldRemoveTagFromItem() throws Exception {
        Item item = new Item();
        item.setName("Stuhl");

        Item savedItem = objectMapper.readValue(mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        Integer tagId1 = createTag("Holz");
        Integer tagId2 = createTag("Sitzmöbel");

        // Assign both tags
        mockMvc.perform(put("/api/items/" + savedItem.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(tagId1, tagId2))))
                .andExpect(status().isOk());

        // Remove one tag
        mockMvc.perform(put("/api/items/" + savedItem.getId() + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(tagId2))))
                .andExpect(status().isOk());

        // Verify only second tag remains
        Item updatedItem = objectMapper.readValue(mockMvc.perform(get("/api/items/" + savedItem.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        assertThat(updatedItem.getTagIds()).containsExactly(tagId2);
        assertThat(updatedItem.getTagIds()).doesNotContain(tagId1);
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

    @Test
    void shouldUploadImageAndAssignToItem() throws Exception {
        // Step 1: Create item
        Item item = new Item();
        item.setName("Schreibtisch");

        Item savedItem = objectMapper.readValue(mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        // Step 2: Prepare test image (simulate file upload)
        byte[] imageData = "fake image content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile imageFile = new MockMultipartFile(
                "files",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                imageData
        );

        // Step 3: Perform multipart upload to add image to item
        mockMvc.perform(multipart("/api/items/" + savedItem.getId() + "/images")
                        .file(imageFile))
                .andExpect(status().isOk());

        // Step 4: Reload item and verify image is assigned
        Item updatedItem = objectMapper.readValue(mockMvc.perform(get("/api/items/" + savedItem.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        assertThat(updatedItem.getImageIds()).isNotNull();
        assertThat(updatedItem.getImageIds().size()).isEqualTo(1);
    }

    @Test
    void shouldUploadMultipleImagesAndAssignToItem() throws Exception {
        // Step 1: Create a new item
        Item item = new Item();
        item.setName("Testgerät");

        String itemJson = objectMapper.writeValueAsString(item);
        String response = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Item createdItem = objectMapper.readValue(response, Item.class);
        Integer itemId = createdItem.getId();

        // Step 2: Prepare multiple test image files
        byte[] imageContent1 = "image1-content".getBytes(StandardCharsets.UTF_8);
        byte[] imageContent2 = "image2-content".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile image1 = new MockMultipartFile(
                "files", "image1.jpg", MediaType.IMAGE_JPEG_VALUE, imageContent1);

        MockMultipartFile image2 = new MockMultipartFile(
                "files", "image2.jpg", MediaType.IMAGE_JPEG_VALUE, imageContent2);

        // Step 3: Perform the upload
        mockMvc.perform(multipart("/api/items/" + itemId + "/images")
                        .file(image1)
                        .file(image2))
                .andExpect(status().isOk());

        // Step 4: Reload item and verify both images are linked
        String updatedResponse = mockMvc.perform(get("/api/items/" + itemId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Item updatedItem = objectMapper.readValue(updatedResponse, Item.class);

        assertThat(updatedItem.getImageIds()).isNotNull();
        assertThat(updatedItem.getImageIds().size()).isEqualTo(2);
    }

    @Test
    void shouldDeleteOneOfMultipleImagesFromItem() throws Exception {
        // Step 1: Create a new item
        Item item = new Item();
        item.setName("Testobjekt mit Bildern");

        Item createdItem = objectMapper.readValue(mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        Integer itemId = createdItem.getId();

        // Step 2: Upload two images
        MockMultipartFile img1 = new MockMultipartFile("files", "img1.jpg", MediaType.IMAGE_JPEG_VALUE, "data1".getBytes());
        MockMultipartFile img2 = new MockMultipartFile("files", "img2.jpg", MediaType.IMAGE_JPEG_VALUE, "data2".getBytes());

        mockMvc.perform(multipart("/api/items/" + itemId + "/images")
                        .file(img1)
                        .file(img2))
                .andExpect(status().isOk());

        // Step 3: Reload item to get image IDs
        Item updatedItem = objectMapper.readValue(mockMvc.perform(get("/api/items/" + itemId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        List<Integer> imageIds = updatedItem.getImageIds().stream().toList();
        assertThat(imageIds).hasSize(2);

        Integer imageToDelete = imageIds.get(0);
        Integer imageToKeep = imageIds.get(1);

        // Step 4: Delete one image
        mockMvc.perform(delete("/api/items/" + itemId + "/images/" + imageToDelete))
                .andExpect(status().isOk());

        // Step 5: Reload item and verify image count
        Item afterDeletion = objectMapper.readValue(mockMvc.perform(get("/api/items/" + itemId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        assertThat(afterDeletion.getImageIds()).containsExactly(imageToKeep);

        // Step 6: Verify the image was really deleted (404 or empty)
        mockMvc.perform(get("/api/images/" + imageToDelete))
                .andExpect(status().isNotFound());
    }


}

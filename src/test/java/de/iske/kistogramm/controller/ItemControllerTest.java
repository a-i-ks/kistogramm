package de.iske.kistogramm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.dto.Room;
import de.iske.kistogramm.dto.Storage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


            if (categoryAttributeTemplateRepository.existsByCategoryIdAndAttributeName(categoryId, attributeName)) {
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
    void setup() throws Exception {
        clothingCategoryId = createCategory("Kleidung");
        createTemplateForCategory("Kleidung", List.of("Größe", "Zuletzt getragen"));
        foodCategoryId = createCategory("Lebensmittel");
        createTemplateForCategory("Lebensmittel", List.of("MHD"));
        electronicCategoryId = createCategory("Elektronik");
    }

    private Integer clothingCategoryId;
    private Integer foodCategoryId;
    private Integer electronicCategoryId;

    private Integer createCategory(String name) throws Exception {
        // check if category already exists
        if (categoryRepository.findByName(name).isPresent()) {
            return categoryRepository.findByName(name).get().getId();
        }

        Map<String, String> cat = Map.of("name", name);
        String response = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cat)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asInt();
    }

    private void createTemplateForCategory(String categoryName, List<String> attributeNames) throws Exception {
        Integer categoryId = categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryName)).getId();
        for (String attributeName : attributeNames) {
            // check if template already exists
            if (templateRepository.existsByCategoryIdAndAttributeName(categoryId, attributeName)) {
                return; // Template already exists
            }
            Map<String, String> template = Map.of("categoryId", categoryId.toString(), "attributeName", attributeName);
            mockMvc.perform(post("/api/categories/template")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(template)))
                    .andExpect(status().isOk());
        }
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
    void whenCreatingClothingItemThenCustomAttributesShouldContainCustomFields() throws Exception {
        Item item = new Item();
        item.setName("Jeans");
        item.setCategoryId(clothingCategoryId);
        item.setQuantity(1);
        item.setCustomAttributes(new HashMap<>());

        String response = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Item created = objectMapper.readValue(response, Item.class);

        assertThat(created.getCustomAttributes()).containsKeys("Größe", "Zuletzt getragen");
    }

    @Test
    void whenChangingCategoryThenNewAttributesShouldBeAddedAndOldShouldPersist() throws Exception {
        Item item = new Item();
        item.setName("Butter");
        item.setCategoryId(clothingCategoryId);
        item.setQuantity(1);
        item.setCustomAttributes(Map.of("Größe", "M"));

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

        assertThat(updated.getCustomAttributes())
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
        item.setCustomAttributes(attributes);

        String response = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Item created = objectMapper.readValue(response, Item.class);

        assertThat(created.getCustomAttributes()).containsEntry("Größe", "L");
        assertThat(Collections.frequency(new ArrayList<>(created.getCustomAttributes().keySet()), "Größe")).isEqualTo(1);
    }

    @Test
    void testCreateItemReturnsBadRequestWithoutName() throws Exception {
        Item item = new Item(); // name is required
        item.setQuantity(1);
        item.setCustomAttributes(new HashMap<>());

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
        assertThat(updatedItem.getImageIds()).hasSize(1);
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
        assertThat(updatedItem.getImageIds()).hasSize(2);
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

    @Test
    void shouldUploadReceiptAndAssignToItem() throws Exception {
        Item item = new Item();
        item.setName("Printer");

        Item savedItem = objectMapper.readValue(mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        MockMultipartFile receiptFile = new MockMultipartFile(
                "files", "receipt.jpg", MediaType.IMAGE_JPEG_VALUE, "receipt".getBytes());

        mockMvc.perform(multipart("/api/items/" + savedItem.getId() + "/receipts")
                        .file(receiptFile))
                .andExpect(status().isOk());

        Item updatedItem = objectMapper.readValue(mockMvc.perform(get("/api/items/" + savedItem.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        assertThat(updatedItem.getReceiptIds()).hasSize(1);
    }

    @Test
    void shouldDeleteReceiptFromItem() throws Exception {
        Item item = new Item();
        item.setName("Phone");

        Item created = objectMapper.readValue(mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        MockMultipartFile r1 = new MockMultipartFile("files", "r1.jpg", MediaType.IMAGE_JPEG_VALUE, "r1".getBytes());
        MockMultipartFile r2 = new MockMultipartFile("files", "r2.jpg", MediaType.IMAGE_JPEG_VALUE, "r2".getBytes());

        mockMvc.perform(multipart("/api/items/" + created.getId() + "/receipts")
                        .file(r1).file(r2))
                .andExpect(status().isOk());

        Item withReceipts = objectMapper.readValue(mockMvc.perform(get("/api/items/" + created.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        List<Integer> receiptIds = withReceipts.getReceiptIds().stream().toList();
        Integer toDelete = receiptIds.get(0);
        Integer toKeep = receiptIds.get(1);

        mockMvc.perform(delete("/api/items/" + created.getId() + "/receipts/" + toDelete))
                .andExpect(status().isOk());

        Item afterDeletion = objectMapper.readValue(mockMvc.perform(get("/api/items/" + created.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Item.class);

        assertThat(afterDeletion.getReceiptIds()).containsExactly(toKeep);
    }

    @Test
    void shouldAssignStorageToItem() throws Exception {
        // Step 1: Optional Raum anlegen
        Room room = new Room();
        room.setName("Keller");

        String roomJson = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Room createdRoom = objectMapper.readValue(roomJson, Room.class);

        // Schritt 1: Erstelle ein Storage
        Storage storage = new Storage();
        storage.setName("Lager A");
        storage.setRoomId(createdRoom.getId());
        storage.setDescription("Im Keller");

        Storage savedStorage = objectMapper.readValue(
                mockMvc.perform(post("/api/storages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(storage)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Storage.class
        );

        // Schritt 2: Erstelle ein Item
        Item item = new Item();
        item.setName("Bohrmaschine");

        Item savedItem = objectMapper.readValue(
                mockMvc.perform(post("/api/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(item)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Item.class
        );

        // Schritt 3: Weise dem Item das Storage zu
        savedItem.setStorageId(savedStorage.getId());

        Item updatedItem = objectMapper.readValue(
                mockMvc.perform(put("/api/items/" + savedItem.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(savedItem)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Item.class
        );

        // Schritt 4: Überprüfe, ob das Storage korrekt zugewiesen wurde
        assertThat(updatedItem.getStorageId()).isEqualTo(savedStorage.getId());
    }

    @Test
    void shouldAssignDifferentStorageToItem() throws Exception {
        // Step 1: Optional Raum anlegen
        Room room = new Room();
        room.setName("Keller");

        String roomJson = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Room createdRoom = objectMapper.readValue(roomJson, Room.class);

        // Storage 1 erstellen
        Storage storageA = new Storage();
        storageA.setName("Regal A");
        storageA.setRoomId(createdRoom.getId());

        Storage savedStorageA = objectMapper.readValue(
                mockMvc.perform(post("/api/storages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(storageA)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Storage.class
        );

        // Storage 2 erstellen
        Storage storageB = new Storage();
        storageB.setName("Regal B");
        storageB.setRoomId(createdRoom.getId());

        Storage savedStorageB = objectMapper.readValue(
                mockMvc.perform(post("/api/storages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(storageB)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Storage.class
        );

        // Item mit Storage A anlegen
        Item item = new Item();
        item.setName("Kabeltrommel");
        item.setStorageId(savedStorageA.getId());

        Item savedItem = objectMapper.readValue(
                mockMvc.perform(post("/api/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(item)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Item.class
        );

        // Storage von Item auf Storage B ändern
        savedItem.setStorageId(savedStorageB.getId());

        Item updatedItem = objectMapper.readValue(
                mockMvc.perform(put("/api/items/" + savedItem.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(savedItem)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Item.class
        );

        // Verifizieren
        assertThat(updatedItem.getStorageId()).isEqualTo(savedStorageB.getId());
        assertThat(updatedItem.getStorageId()).isNotEqualTo(savedStorageA.getId());
        assertThat(updatedItem.getDateModified()).isNotNull();
    }


    @Test
    void shouldUnassignStorageFromItem() throws Exception {
        // Step 1: Optional Raum anlegen
        Room room = new Room();
        room.setName("Keller");

        String roomJson = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Room createdRoom = objectMapper.readValue(roomJson, Room.class);

        // Schritt 1: Erstelle ein Storage
        Storage storage = new Storage();
        storage.setName("Werkbank");
        storage.setRoomId(createdRoom.getId());
        storage.setDescription("In der Garage");

        Storage savedStorage = objectMapper.readValue(
                mockMvc.perform(post("/api/storages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(storage)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Storage.class
        );

        // Schritt 2: Erstelle ein Item mit zugewiesenem Storage
        Item item = new Item();
        item.setName("Schraubendreher");
        item.setStorageId(savedStorage.getId());

        Item savedItem = objectMapper.readValue(
                mockMvc.perform(post("/api/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(item)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Item.class
        );

        // Schritt 3: Entferne das Storage (setze storageId = null)
        savedItem.setStorageId(null);

        Item updatedItem = objectMapper.readValue(
                mockMvc.perform(put("/api/items/" + savedItem.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(savedItem)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Item.class
        );

        // Schritt 4: Überprüfe, dass kein Storage mehr zugewiesen ist
        assertThat(updatedItem.getStorageId()).isNull();
        assertThat(updatedItem.getDateModified()).isNotNull();
    }
}

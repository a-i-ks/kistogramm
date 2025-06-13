package de.iske.kistogramm.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.Image;
import de.iske.kistogramm.dto.Room;
import de.iske.kistogramm.dto.Storage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StorageControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateStorageSuccessfully() throws Exception {
        // Step 1: Optional Raum anlegen
        Room room = new Room();
        room.setName("Keller");

        String roomJson = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Room createdRoom = objectMapper.readValue(roomJson, Room.class);

        // Step 2: Storage erstellen mit Verknüpfung zu Room
        Storage storage = new Storage();
        storage.setName("Werkzeugkiste");
        storage.setDescription("Enthält Hammer, Schraubenzieher usw.");
        storage.setRoomId(createdRoom.getId());

        String storageJson = mockMvc.perform(post("/api/storages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storage)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Storage createdStorage = objectMapper.readValue(storageJson, Storage.class);

        // Step 3: Validierung
        assertThat(createdStorage.getId()).isNotNull();
        assertThat(createdStorage.getName()).isEqualTo("Werkzeugkiste");
        assertThat(createdStorage.getRoomId()).isEqualTo(createdRoom.getId());
    }

    @Test
    void shouldUpdateStorageSuccessfully() throws Exception {
        // Step 1: Erstelle einen Raum
        Room room = new Room();
        room.setName("Lager");

        Room createdRoom = objectMapper.readValue(
                mockMvc.perform(post("/api/rooms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(room)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Room.class
        );

        // Step 2: Erstelle einen Storage in diesem Raum
        Storage storage = new Storage();
        storage.setName("Alte Kiste");
        storage.setDescription("Beinhaltet alte Kabel");
        storage.setRoomId(createdRoom.getId());

        Storage createdStorage = objectMapper.readValue(
                mockMvc.perform(post("/api/storages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(storage)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Storage.class
        );

        // Step 3: Ändere den Storage-Namen und Beschreibung
        createdStorage.setName("Werkzeugbox");
        createdStorage.setDescription("Beinhaltet Schraubenschlüssel und Zangen");

        Storage updatedStorage = objectMapper.readValue(
                mockMvc.perform(put("/api/storages/" + createdStorage.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createdStorage)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Storage.class
        );

        // Step 4: Validierung
        assertThat(updatedStorage.getName()).isEqualTo("Werkzeugbox");
        assertThat(updatedStorage.getDescription()).isEqualTo("Beinhaltet Schraubenschlüssel und Zangen");
    }

    @Test
    void shouldDeleteStorageSuccessfully() throws Exception {
        // Step 1: Erstelle einen Raum, da Storage einen Raum benötigt
        Room room = new Room();
        room.setName("Lagerraum");

        Room createdRoom = objectMapper.readValue(
                mockMvc.perform(post("/api/rooms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(room)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Room.class
        );

        // Step 2: Erstelle einen Storage in diesem Raum
        Storage storage = new Storage();
        storage.setName("Regal");
        storage.setDescription("Werkzeugablage");
        storage.setRoomId(createdRoom.getId());

        Storage createdStorage = objectMapper.readValue(
                mockMvc.perform(post("/api/storages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(storage)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Storage.class
        );

        // Step 3: Lösche den Storage
        mockMvc.perform(delete("/api/storages/" + createdStorage.getId()))
                .andExpect(status().isNoContent());

        // Step 4: Überprüfe, dass der Storage nicht mehr vorhanden ist
        mockMvc.perform(get("/api/storages/" + createdStorage.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUploadImageToStorageAndRetrieveIt() throws Exception {
        // Step 1: Create Room
        Room room = new Room();
        room.setName("Keller");

        Room savedRoom = objectMapper.readValue(mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Room.class);

        // Step 2: Create Storage
        Storage storage = new Storage();
        storage.setName("Geräteschrank");
        storage.setRoomId(savedRoom.getId());

        Storage savedStorage = objectMapper.readValue(mockMvc.perform(post("/api/storages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storage)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Storage.class);

        // Step 3: Upload image
        byte[] imageData = "fake image content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile imageFile = new MockMultipartFile(
                "files",
                "image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                imageData
        );

        Storage updatedStorage = objectMapper.readValue(mockMvc.perform(multipart("/api/storages/" + savedStorage.getId() + "/images")
                        .file(imageFile))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Storage.class);

        // Step 4: Check if image ID was assigned
        assertThat(updatedStorage.getImageIds()).isNotNull().hasSize(1);

        Integer imageId = updatedStorage.getImageIds().getFirst();

        // Step 5: Retrieve the image by ID
        var image = objectMapper.readValue(mockMvc.perform(get("/api/images/" + imageId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Image.class);
        assertThat(image.getId()).isEqualTo(imageId);
        assertThat(image.getData()).isNotNull(); // Ensure image data is not null

        // Check if image is available via storage api path
        mockMvc.perform(get("/api/storages/" + savedStorage.getId() + "/images/" + imageId))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    Image image1 = objectMapper.readValue(result.getResponse().getContentAsString(), Image.class);
                    assertThat(image1.getUuid()).isEqualTo(image.getUuid());
                });
    }

    @Test
    void shouldUploadMultipleImagesToStorageAndVerifyAccessViaEndpoints() throws Exception {
        // Step 1: Create a room for the storage
        var roomJson = """
                {
                  "name": "Keller",
                  "description": "Vorratsraum"
                }
                """;

        String roomResponse = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int roomId = objectMapper.readTree(roomResponse).get("id").asInt();

        // Step 2: Create a storage
        String storageJson = """
                {
                  "name": "Regal 1",
                  "description": "Regal für Vorräte",
                  "roomId": %d
                }
                """.formatted(roomId);

        String storageResponse = mockMvc.perform(post("/api/storages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(storageJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Storage createdStorage = objectMapper.readValue(storageResponse, Storage.class);
        Integer storageId = createdStorage.getId();

        // Step 3: Upload multiple images
        MockMultipartFile image1 = new MockMultipartFile("files", "image1.jpg", MediaType.IMAGE_JPEG_VALUE, "img1content".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile image2 = new MockMultipartFile("files", "image2.jpg", MediaType.IMAGE_JPEG_VALUE, "img2content".getBytes(StandardCharsets.UTF_8));

        String updatedStorageJson = mockMvc.perform(multipart("/api/storages/" + storageId + "/images")
                        .file(image1)
                        .file(image2))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Storage updatedStorage = objectMapper.readValue(updatedStorageJson, Storage.class);

        assertThat(updatedStorage.getImageIds()).hasSize(2);

        Integer imageId1 = updatedStorage.getImageIds().get(0);
        Integer imageId2 = updatedStorage.getImageIds().get(1);

        // Step 4a: Verify images via /api/storages/<id>
        mockMvc.perform(get("/api/storages/" + storageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageIds.length()").value(2));

        // Step 4b: Verify all images via /api/storages/<id>/images
        mockMvc.perform(get("/api/storages/" + storageId + "/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Step 4c: Verify one image via /api/storages/<id>/images/<imageId>
        mockMvc.perform(get("/api/storages/" + storageId + "/images/" + imageId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(imageId1));

        // Step 4d: Verify image via /api/images/<imageId>
        mockMvc.perform(get("/api/images/" + imageId2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(imageId2));
    }

    @Test
    void shouldUploadTwoImagesAndDeleteOneFromStorage() throws Exception {
        // Step 1: Create a room
        Room room = new Room();
        room.setName("Raum mit Bildern");
        room.setDescription("Für Storage-Test");

        Room savedRoom = objectMapper.readValue(mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Room.class);

        // Step 2: Create a storage in that room
        Storage storage = new Storage();
        storage.setName("Bildlager");
        storage.setDescription("Hier kommen Bilder hin");
        storage.setRoomId(savedRoom.getId());

        Storage savedStorage = objectMapper.readValue(mockMvc.perform(post("/api/storages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storage)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Storage.class);

        // Step 3: Upload two images
        MockMultipartFile file1 = new MockMultipartFile("files", "bild1.jpg", MediaType.IMAGE_JPEG_VALUE, "image1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "bild2.jpg", MediaType.IMAGE_JPEG_VALUE, "image2".getBytes());

        Storage withImages = objectMapper.readValue(mockMvc.perform(multipart("/api/storages/" + savedStorage.getId() + "/images")
                        .file(file1)
                        .file(file2))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), Storage.class);

        assertThat(withImages.getImageIds()).hasSize(2);

        Integer imageIdToDelete = withImages.getImageIds().get(0);
        Integer imageIdToKeep = withImages.getImageIds().get(1);

        // Step 4: Delete one image
        mockMvc.perform(delete("/api/storages/" + savedStorage.getId() + "/images/" + imageIdToDelete))
                .andExpect(status().isOk());

        // Step 5: Validate image list now only contains one
        MvcResult result = mockMvc.perform(get("/api/storages/" + savedStorage.getId() + "/images"))
                .andExpect(status().isOk())
                .andReturn();

        Image[] remainingImages = objectMapper.readValue(result.getResponse().getContentAsString(), Image[].class);
        assertThat(remainingImages).hasSize(1);
        assertThat(remainingImages[0].getId()).isEqualTo(imageIdToKeep);

        // Step 6: Deleted image is not accessible
        mockMvc.perform(get("/api/images/" + imageIdToDelete))
                .andExpect(status().isNotFound());

        // Step 7: Remaining image is still accessible
        mockMvc.perform(get("/api/images/" + imageIdToKeep))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeleteAllImagesFromStorage() throws Exception {
        // Step 1: Create Room (storage requires a room)
        Room room = new Room();
        room.setName("Test Room");
        Room savedRoom = objectMapper.readValue(
                mockMvc.perform(post("/api/rooms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(room)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Room.class
        );

        // Step 2: Create Storage
        Storage storage = new Storage();
        storage.setName("Storage with Images");
        storage.setRoomId(savedRoom.getId());

        Storage savedStorage = objectMapper.readValue(
                mockMvc.perform(post("/api/storages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(storage)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Storage.class
        );

        // Step 3: Upload two images
        MockMultipartFile image1 = new MockMultipartFile("files", "image1.jpg", MediaType.IMAGE_JPEG_VALUE, "image1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("files", "image2.jpg", MediaType.IMAGE_JPEG_VALUE, "image2".getBytes());

        mockMvc.perform(multipart("/api/storages/" + savedStorage.getId() + "/images")
                        .file(image1)
                        .file(image2))
                .andExpect(status().isOk());

        // Step 4: Verify that 2 images exist
        List<Image> imagesBeforeDelete = objectMapper.readValue(
                mockMvc.perform(get("/api/storages/" + savedStorage.getId() + "/images"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );

        assertThat(imagesBeforeDelete).hasSize(2);

        // Step 5: Delete all images
        mockMvc.perform(delete("/api/storages/" + savedStorage.getId() + "/images"))
                .andExpect(status().isOk());

        // Step 6: Verify that no images remain
        List<Image> imagesAfterDelete = objectMapper.readValue(
                mockMvc.perform(get("/api/storages/" + savedStorage.getId() + "/images"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );

        assertThat(imagesAfterDelete).isEmpty();
    }
}

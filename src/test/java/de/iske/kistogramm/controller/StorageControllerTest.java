package de.iske.kistogramm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.Room;
import de.iske.kistogramm.dto.Storage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
public class StorageControllerTest {

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
        mockMvc.perform(get("/api/images/" + imageId))
                .andExpect(status().isOk());
        //        .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }
}

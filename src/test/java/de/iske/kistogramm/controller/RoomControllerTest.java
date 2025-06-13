package de.iske.kistogramm.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.Image;
import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.dto.Room;
import de.iske.kistogramm.dto.Storage;
class RoomControllerTest extends AbstractControllerTest {
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class RoomControllerTest {

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

    @Test
    void shouldCreateRoomSuccessfully() throws Exception {
        // Step 1: Raum-Daten vorbereiten
        Room room = new Room();
        room.setName("Keller");
        room.setDescription("Lagerraum im Untergeschoss");

        // Step 2: Raum anlegen
        String response = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Room saved = objectMapper.readValue(response, Room.class);

        // Step 3: Validierung
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Keller");
        assertThat(saved.getDescription()).isEqualTo("Lagerraum im Untergeschoss");
        assertThat(saved.getDateAdded()).isNotNull();
        assertThat(saved.getDateModified()).isNotNull();
    }

    @Test
    void shouldUpdateRoomSuccessfully() throws Exception {
        // Step 1: Neuen Raum erstellen
        Room original = new Room();
        original.setName("Abstellkammer");
        original.setDescription("Kleiner Raum unter der Treppe");

        String createdResponse = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(original)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Room savedRoom = objectMapper.readValue(createdResponse, Room.class);

        // Step 2: Raumdaten ändern
        Room updatedRoom = new Room();
        updatedRoom.setName("Hauswirtschaftsraum");
        updatedRoom.setDescription("Jetzt mit Waschmaschine und Trockner");

        // Step 3: PUT-Request zum Ändern senden
        String updatedResponse = mockMvc.perform(put("/api/rooms/" + savedRoom.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedRoom)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Room result = objectMapper.readValue(updatedResponse, Room.class);

        // Step 4: Validierung
        assertThat(result.getId()).isEqualTo(savedRoom.getId());
        assertThat(result.getName()).isEqualTo("Hauswirtschaftsraum");
        assertThat(result.getDescription()).isEqualTo("Jetzt mit Waschmaschine und Trockner");
        assertThat(result.getDateModified()).isAfter(savedRoom.getDateModified());
    }

    @Test
    void shouldReturnAllCreatedRooms() throws Exception {
        // Step 1: Zwei Räume anlegen
        Room room1 = new Room();
        room1.setName("Wohnzimmer");
        room1.setDescription("Raum mit Sofa und Fernseher");

        Room room2 = new Room();
        room2.setName("Schlafzimmer");
        room2.setDescription("Raum mit Bett und Schrank");

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room2)))
                .andExpect(status().isOk());

        // Step 2: GET all rooms
        String response = mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Room> roomList = objectMapper.readValue(response, new TypeReference<>() {
        });

        // Step 3: Überprüfen, ob beide Räume enthalten sind
        List<String> names = roomList.stream().map(Room::getName).toList();
        assertThat(names).contains("Wohnzimmer", "Schlafzimmer");
    }

    @Test
    void shouldDeleteOneRoomAndRetainTheOther() throws Exception {
        // Step 1: Zwei Räume anlegen
        Room room1 = new Room();
        room1.setName("Badezimmer");
        room1.setDescription("Raum mit Dusche");

        Room room2 = new Room();
        room2.setName("Esszimmer");
        room2.setDescription("Raum mit Esstisch");

        String response1 = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room1)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String response2 = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room2)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Room savedRoom1 = objectMapper.readValue(response1, Room.class);
        Room savedRoom2 = objectMapper.readValue(response2, Room.class);

        // Step 2: Einen Raum löschen
        mockMvc.perform(delete("/api/rooms/" + savedRoom1.getId()))
                .andExpect(status().isNoContent());

        // Step 3: Alle Räume abfragen
        String allResponse = mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Room> roomList = objectMapper.readValue(allResponse, new TypeReference<>() {
        });

        // Step 4: Validierung – nur noch der zweite Raum ist da
        List<Integer> roomIds = roomList.stream().map(Room::getId).toList();
        assertThat(roomIds).contains(savedRoom2.getId()).doesNotContain(savedRoom1.getId());
    }

    @Test
    void shouldReturnStoragesAssignedToSpecificRoom() throws Exception {
        // Raum A anlegen
        Room roomA = new Room();
        roomA.setName("Raum A");
        roomA.setDescription("Erster Raum");

        Room savedRoomA = objectMapper.readValue(
                mockMvc.perform(post("/api/rooms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(roomA)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Room.class);

        // Raum B anlegen
        Room roomB = new Room();
        roomB.setName("Raum B");
        roomB.setDescription("Zweiter Raum");

        Room savedRoomB = objectMapper.readValue(
                mockMvc.perform(post("/api/rooms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(roomB)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Room.class);

        // 5 Storages anlegen
        for (int i = 1; i <= 5; i++) {
            Storage storage = new Storage();
            storage.setName("Storage " + i);
            storage.setDescription("Beschreibung " + i);

            // Storage 1-2 in Room A, 3-5 in Room B
            if (i <= 2) {
                storage.setRoomId(savedRoomA.getId());
            } else {
                storage.setRoomId(savedRoomB.getId());
            }

            mockMvc.perform(post("/api/storages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(storage)))
                    .andExpect(status().isOk());
        }

        // Abfrage aller Storages in Room A
        String responseA = mockMvc.perform(get("/api/rooms/" + savedRoomA.getId() + "/storages"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Storage> storagesInRoomA = objectMapper.readValue(responseA, new TypeReference<>() {
        });
        assertThat(storagesInRoomA).hasSize(2).allMatch(s -> s.getRoomId().equals(savedRoomA.getId()));

        // Abfrage aller Storages in Room B
        String responseB = mockMvc.perform(get("/api/rooms/" + savedRoomB.getId() + "/storages"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Storage> storagesInRoomB = objectMapper.readValue(responseB, new TypeReference<>() {
        });
        assertThat(storagesInRoomB).hasSize(3).allMatch(s -> s.getRoomId().equals(savedRoomB.getId()));
    }

    @Test
    void shouldReturnCorrectItemsByRoomId() throws Exception {
        // 1. Räume anlegen
        Room room1 = new Room();
        room1.setName("Raum 1");
        room1.setDescription("Beschreibung Raum 1");

        Room savedRoom1 = objectMapper.readValue(
                mockMvc.perform(post("/api/rooms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(room1)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Room.class);

        Room room2 = new Room();
        room2.setName("Raum 2");
        room2.setDescription("Beschreibung Raum 2");

        Room savedRoom2 = objectMapper.readValue(
                mockMvc.perform(post("/api/rooms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(room2)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Room.class);

        // 2. Storages anlegen
        Storage storageA = new Storage();
        storageA.setName("Storage A");
        storageA.setRoomId(savedRoom1.getId());
        Storage savedA = objectMapper.readValue(
                mockMvc.perform(post("/api/storages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(storageA)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Storage.class);

        Storage storageB = new Storage();
        storageB.setName("Storage B");
        storageB.setRoomId(savedRoom1.getId());
        Storage savedB = objectMapper.readValue(
                mockMvc.perform(post("/api/storages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(storageB)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Storage.class);

        Storage storageC = new Storage();
        storageC.setName("Storage C");
        storageC.setRoomId(savedRoom2.getId());
        Storage savedC = objectMapper.readValue(
                mockMvc.perform(post("/api/storages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(storageC)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Storage.class);

        // 3. Items anlegen (2 in A, 2 in B, 2 in C)
        for (int i = 1; i <= 6; i++) {
            Item item = new Item();
            item.setName("Item " + i);

            if (i <= 2) {
                item.setStorageId(savedA.getId());
            } else if (i <= 4) {
                item.setStorageId(savedB.getId());
            } else {
                item.setStorageId(savedC.getId());
            }

            mockMvc.perform(post("/api/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(item)))
                    .andExpect(status().isOk());
        }

        // 4. Abruf aller Items aus Raum 1
        String json = mockMvc.perform(get("/api/rooms/" + savedRoom1.getId() + "/items"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Item> itemsInRoom1 = objectMapper.readValue(json, new TypeReference<>() {
        });
        assertThat(itemsInRoom1).hasSize(4);
        assertThat(itemsInRoom1).extracting(Item::getName)
                .containsExactlyInAnyOrder("Item 1", "Item 2", "Item 3", "Item 4");
    }

    @Test
    void shouldNotCreateStorageWithoutRoom() throws Exception {
        // Erstelle ein Storage-Objekt ohne roomId
        Storage storage = new Storage();
        storage.setName("Werkzeugkiste");
        storage.setDescription("Enthält Schraubendreher und Hammer");

        // Versuche, den Storage ohne roomId anzulegen
        mockMvc.perform(post("/api/storages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storage)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("roomId")));
    }

    @Test
    void shouldUploadImageToRoomAndVerifyAccess() throws Exception {
        // 1. Raum anlegen
        Room room = new Room();
        room.setName("Wohnzimmer");
        room.setDescription("Hauptwohnraum");

        Room savedRoom = objectMapper.readValue(
                mockMvc.perform(post("/api/rooms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(room)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Room.class
        );

        // 2. Bild erzeugen
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "wohnzimmer.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image-content".getBytes(StandardCharsets.UTF_8)
        );

        // 3. Upload durchführen
        Room updatedRoom = objectMapper.readValue(
                mockMvc.perform(multipart("/api/rooms/" + savedRoom.getId() + "/image")
                                .file(imageFile))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Room.class
        );

        // 4. Bild-ID im Raum prüfen
        assertThat(updatedRoom.getImageId()).isNotNull();

        // 5. Bild abrufen über: /api/rooms/{roomId}/image
        Image imageByRoom = objectMapper.readValue(
                mockMvc.perform(get("/api/rooms/" + savedRoom.getId() + "/image"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Image.class
        );

        assertThat(imageByRoom.getId()).isEqualTo(updatedRoom.getImageId());

        // 6. Bild abrufen über: /api/images/{id}
        Image imageById = objectMapper.readValue(
                mockMvc.perform(get("/api/images/" + imageByRoom.getId()))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Image.class
        );

        assertThat(imageById.getId()).isEqualTo(imageByRoom.getId());
    }

    @Test
    void shouldUploadAndDeleteRoomImageSuccessfully() throws Exception {
        // 1. Raum anlegen
        Room room = new Room();
        room.setName("Badezimmer");
        room.setDescription("Mit Dusche");

        Room savedRoom = objectMapper.readValue(
                mockMvc.perform(post("/api/rooms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(room)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Room.class
        );

        // 2. Bild vorbereiten und hochladen
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "bathroom.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "dummy-image-data".getBytes(StandardCharsets.UTF_8)
        );

        Room roomWithImage = objectMapper.readValue(
                mockMvc.perform(multipart("/api/rooms/" + savedRoom.getId() + "/image")
                                .file(imageFile))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Room.class
        );

        Integer imageId = roomWithImage.getImageId();
        assertThat(imageId).isNotNull();

        // 3. Bild ist abrufbar über /api/rooms/{roomId}/image
        mockMvc.perform(get("/api/rooms/" + savedRoom.getId() + "/image"))
                .andExpect(status().isOk());

        // 4. Bild ist abrufbar über /api/images/{imageId}
        mockMvc.perform(get("/api/images/" + imageId))
                .andExpect(status().isOk());

        // 5. Bild löschen
        mockMvc.perform(delete("/api/rooms/" + savedRoom.getId() + "/image"))
                .andExpect(status().isNoContent());

        // 6. Bild nicht mehr abrufbar
        mockMvc.perform(get("/api/rooms/" + savedRoom.getId() + "/image"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/images/" + imageId))
                .andExpect(status().isNotFound());
    }
}

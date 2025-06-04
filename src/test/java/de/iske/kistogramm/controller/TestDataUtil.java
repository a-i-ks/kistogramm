package de.iske.kistogramm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TestDataUtil {

    public static int createRoomWithImage(MockMvc mockMvc, ObjectMapper objectMapper, String name, String description) throws Exception {
        // Step 1: Raum anlegen
        var room = new Room();
        room.setName(name);
        room.setDescription(description);

        String response = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var createdRoom = objectMapper.readValue(response, Room.class);
        int roomId = createdRoom.getId();

        // Step 2: Bild hochladen
        byte[] imageData = ("Bild für Raum: " + name).getBytes(StandardCharsets.UTF_8);
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "raum-bild.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                imageData
        );

        mockMvc.perform(multipart("/api/rooms/" + roomId + "/image")
                        .file(imageFile))
                .andExpect(status().isOk());

        return roomId;
    }

    public static int createStorageWithImage(MockMvc mockMvc, ObjectMapper objectMapper, String name, String description, int roomId) throws Exception {
        // Step 1: Storage anlegen
        var storage = new Storage();
        storage.setName(name);
        storage.setDescription(description);
        storage.setRoomId(roomId);

        String response = mockMvc.perform(post("/api/storages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storage)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var createdStorage = objectMapper.readValue(response, Storage.class);
        int storageId = createdStorage.getId();

        // Step 2: Bild hochladen

        byte[] imageData = ("Bild für Storage: " + name).getBytes(StandardCharsets.UTF_8);
        MockMultipartFile imageFile = new MockMultipartFile(
                "files",
                "storage-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                imageData
        );

        mockMvc.perform(multipart("/api/storages/" + storageId + "/images")
                        .file(imageFile))
                .andExpect(status().isOk());

        return storageId;
    }

    public static int createTag(MockMvc mockMvc, ObjectMapper objectMapper, String name) throws Exception {
        var tag = new Tag();
        tag.setName(name);
        String response = mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(tag)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var createdTag = objectMapper.readValue(response, Storage.class);

        return createdTag.getId();
    }

    public static int createCategoryWithTemplate(MockMvc mockMvc, ObjectMapper objectMapper, String name, List<String> attributeNames) throws Exception {
        // Step 1: Erstelle die Kategorie selbst
        var category = new Category();
        category.setName(name);

        String categoryResponse = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(category)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int categoryId = objectMapper.readValue(categoryResponse, Category.class).getId();

        // Step 2: Füge die Attribute Templates hinzu
        for (String attr : attributeNames) {
            CategoryAttributeTemplate template = new CategoryAttributeTemplate();
            template.setCategoryId(categoryId);
            template.setAttributeName(attr);

            mockMvc.perform(post("/api/categories/template")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(template)))
                    .andExpect(status().isOk());
        }

        return categoryId;
    }

    public static int createItemWithDetails(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            String name,
            String description,
            Integer storageId,
            Integer categoryId,
            Map<String, String> customAttributes,
            List<Integer> tagIds,
            Integer numberOfImages
    ) throws Exception {
        // 1. Erstelle Item DTO
        Item itemDto = new Item();
        itemDto.setName(name);
        itemDto.setDescription(description);
        itemDto.setStorageId(storageId);
        itemDto.setCategoryId(categoryId);
        itemDto.setTagIds(new HashSet<>(tagIds));
        itemDto.setCustomAttributes(customAttributes);

        // 2. Sende Item-POST Request
        String itemResponse = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int itemId = objectMapper.readValue(itemResponse, Item.class).getId();

        // 3. Bilder hochladen
        for (int i = 0; i < numberOfImages; i++) {
            byte[] imageData = ("fake image content " + i).getBytes(StandardCharsets.UTF_8);
            MockMultipartFile imageFile = new MockMultipartFile(
                    "files", // wichtig: plural muss zum Controller-Parameter passen
                    "image" + i + ".jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    imageData
            );

            mockMvc.perform(multipart("/api/items/" + itemId + "/images")
                            .file(imageFile))
                    .andExpect(status().isOk());
        }

        return itemId;
    }
}

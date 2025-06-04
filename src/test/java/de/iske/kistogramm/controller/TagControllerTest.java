package de.iske.kistogramm.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.dto.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class TagControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateNewTagSuccessfully() throws Exception {
        // Tag vorbereiten
        Tag tag = new Tag();
        tag.setName("Reparieren");

        // Tag per POST anlegen
        String response = mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tag)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Ergebnis prüfen
        Tag savedTag = objectMapper.readValue(response, Tag.class);

        assertThat(savedTag.getId()).isNotNull();
        assertThat(savedTag.getName()).isEqualTo("Reparieren");
        assertThat(savedTag.getDateAdded()).isNotNull();
        assertThat(savedTag.getDateModified()).isNotNull();
    }

    @Test
    void shouldUpdateTagNameSuccessfully() throws Exception {
        // 1. Neues Tag anlegen
        Tag tag = new Tag();
        tag.setName("Werkzeug");

        String createResponse = mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tag)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Tag createdTag = objectMapper.readValue(createResponse, Tag.class);

        // 2. Tag aktualisieren
        createdTag.setName("Werkstatt");

        String updateResponse = mockMvc.perform(put("/api/tags/" + createdTag.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createdTag)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Tag updatedTag = objectMapper.readValue(updateResponse, Tag.class);

        // 3. Überprüfen
        assertThat(updatedTag.getId()).isEqualTo(createdTag.getId());
        assertThat(updatedTag.getName()).isEqualTo("Werkstatt");
        assertThat(updatedTag.getDateModified()).isAfter(createdTag.getDateModified());
    }

    @Test
    void shouldDeleteTagSuccessfully() throws Exception {
        // 1. Tag anlegen
        Tag tag = new Tag();
        tag.setName("Lager");

        String createResponse = mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tag)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Tag createdTag = objectMapper.readValue(createResponse, Tag.class);

        // 2. Tag löschen
        mockMvc.perform(delete("/api/tags/" + createdTag.getId()))
                .andExpect(status().isNoContent());

        // 3. Sicherstellen, dass es nicht mehr abrufbar ist
        mockMvc.perform(get("/api/tags/" + createdTag.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldOnlyAllowDeletingTagIfUnassignedFromItem() throws Exception {
        // 1. Tag anlegen
        Tag tag = new Tag();
        tag.setName("Saison");

        String tagResponse = mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tag)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Tag createdTag = objectMapper.readValue(tagResponse, Tag.class);

        // 2. Item anlegen
        Item item = new Item();
        item.setName("Winterjacke");
        item.setTagIds(Set.of(createdTag.getId()));

        String itemResponse = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Item createdItem = objectMapper.readValue(itemResponse, Item.class);

        // 3. Versuch, Tag zu löschen → sollte fehlschlagen (409 Conflict)
        mockMvc.perform(delete("/api/tags/" + createdTag.getId()))
                .andExpect(status().isBadRequest());

        // 4. Tag vom Item entfernen
        createdItem.setTagIds(Set.of());
        mockMvc.perform(put("/api/items/" + createdItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createdItem)))
                .andExpect(status().isOk());

        // 5. Jetzt Tag löschen → sollte funktionieren
        mockMvc.perform(delete("/api/tags/" + createdTag.getId()))
                .andExpect(status().isNoContent());

        // 6. Sicherstellen, dass der Tag entfernt wurde
        mockMvc.perform(get("/api/tags/" + createdTag.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateMultipleTagsAndReturnAllViaGet() throws Exception {
        // Step 1: Drei Tags anlegen
        List<String> tagNames = List.of("Büro", "Werkzeug", "Sommer");

        for (String name : tagNames) {
            Tag tag = new Tag();
            tag.setName(name);

            mockMvc.perform(post("/api/tags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tag)))
                    .andExpect(status().isOk());
        }

        // Step 2: Alle Tags abfragen
        String response = mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Tag> resultTags = objectMapper.readValue(response, new TypeReference<>() {
        });

        // Step 3: Prüfung
        List<String> actualNames = resultTags.stream()
                .map(Tag::getName)
                .collect(Collectors.toList());

        assertThat(actualNames).containsAll(tagNames);
    }

    @Test
    void shouldGetAllItemsByTagId() throws Exception {
        // Step 1: Tags anlegen
        Tag tag1 = createTag("Tag1");
        Tag tag2 = createTag("Tag2");
        Tag tag3 = createTag("Tag3");

        // Step 2: Drei Items anlegen
        Item item1 = createItem("Item 1");
        Item item2 = createItem("Item 2");
        Item item3 = createItem("Item 3");

        // Step 3: Tags den Items zuweisen
        assignTags(item1.getId(), List.of(tag1.getId(), tag2.getId()));
        assignTags(item2.getId(), List.of(tag2.getId(), tag3.getId()));
        assignTags(item3.getId(), List.of(tag1.getId()));

        // Step 4: Abfrage aller Items mit Tag1
        String json = mockMvc.perform(get("/api/tags/" + tag1.getId() + "/items"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Item> result = objectMapper.readValue(json, new TypeReference<>() {
        });
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(Item::getName).toList())
                .containsExactlyInAnyOrder("Item 1", "Item 3");
    }

    private Tag createTag(String name) throws Exception {
        Tag tag = new Tag();
        tag.setName(name);
        String json = mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tag)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, Tag.class);
    }

    private Item createItem(String name) throws Exception {
        Item item = new Item();
        item.setName(name);
        String json = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, Item.class);
    }

    private void assignTags(Integer itemId, List<Integer> tagIds) throws Exception {
        mockMvc.perform(put("/api/items/" + itemId + "/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tagIds)))
                .andExpect(status().isOk());
    }
}

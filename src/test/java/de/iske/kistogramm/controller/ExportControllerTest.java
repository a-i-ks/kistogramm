package de.iske.kistogramm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.export.ExportImage;
import de.iske.kistogramm.dto.export.ExportResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExportCompleteDataset() throws Exception {
        // Step 1: Create rooms with images
        int wohnzimmerId = TestDataUtil.createRoomWithImage(mockMvc, objectMapper, "Wohnzimmer", "Raum mit Sofa");
        int schlafzimmerId = TestDataUtil.createRoomWithImage(mockMvc, objectMapper, "Schlafzimmer", "Mit Bett");
        int barId = TestDataUtil.createRoomWithImage(mockMvc, objectMapper, "Bar", "Raum mit Getränken");

        // Step 2: Create storages with images and assign to rooms
        int regalId = TestDataUtil.createStorageWithImage(mockMvc, objectMapper, "Regal", "Für Bücher", wohnzimmerId);
        int schrankId = TestDataUtil.createStorageWithImage(mockMvc, objectMapper, "Schrank", "Kleidung", schlafzimmerId);
        int minibarId = TestDataUtil.createStorageWithImage(mockMvc, objectMapper, "Minibar", "Getränke", barId);
        int kisteId = TestDataUtil.createStorageWithImage(mockMvc, objectMapper, "Kiste", "Sonstiges", wohnzimmerId);

        // Step 3: Create tags
        int tagWerkzeug = TestDataUtil.createTag(mockMvc, objectMapper, "Werkzeug");
        int tagKueche = TestDataUtil.createTag(mockMvc, objectMapper, "Küche");
        int tagGarten = TestDataUtil.createTag(mockMvc, objectMapper, "Garten");
        int tagDeko = TestDataUtil.createTag(mockMvc, objectMapper, "Deko");
        int tagSport = TestDataUtil.createTag(mockMvc, objectMapper, "Sport");

        // Step 4: Create category with templates
        int kategorieBuchId = TestDataUtil.createCategoryWithTemplate(mockMvc, objectMapper, "Buch", List.of("Autor", "Verlag"));

        // Step 5: Create items with images, assign storages/tags/categories/customAttributes
        TestDataUtil.createItemWithDetails(mockMvc, objectMapper, "Roman", "Historisch", regalId, kategorieBuchId,
                Map.of("Autor", "M. Mustermann"), List.of(tagDeko), 2);

        TestDataUtil.createItemWithDetails(mockMvc, objectMapper, "Hantelset", "20kg", kisteId, null,
                Map.of("Farbe", "Schwarz"), List.of(tagSport), 1);

        TestDataUtil.createItemWithDetails(mockMvc, objectMapper, "Lampe", "LED", regalId, null,
                Map.of(), List.of(tagDeko), 1);

        TestDataUtil.createItemWithDetails(mockMvc, objectMapper, "Mixer", "Küchengerät", minibarId, null,
                Map.of(), List.of(tagKueche), 1);

        TestDataUtil.createItemWithDetails(mockMvc, objectMapper, "Bettwäsche", "Baumwolle", schrankId, null,
                Map.of(), List.of(tagGarten), 1);

        TestDataUtil.createItemWithDetails(mockMvc, objectMapper, "Werkzeugkasten", "Vollausstattung", kisteId, null,
                Map.of(), List.of(tagWerkzeug), 2);

        TestDataUtil.createItemWithDetails(mockMvc, objectMapper, "Kochbuch", "Vegetarisch", minibarId, kategorieBuchId,
                Map.of("Autor", "T. Tofu"), List.of(tagKueche), 1);

        TestDataUtil.createItemWithDetails(mockMvc, objectMapper, "Poster", "Kunst", regalId, null,
                Map.of(), List.of(tagDeko), 1);

        TestDataUtil.createItemWithDetails(mockMvc, objectMapper, "Trinkglas", "Kristall", minibarId, null,
                Map.of(), List.of(tagDeko), 1);

        TestDataUtil.createItemWithDetails(mockMvc, objectMapper, "Fernbedienung", "TV", regalId, null,
                Map.of(), List.of(tagDeko), 1);

        // Step 6: Perform export
        MvcResult result = mockMvc.perform(get("/api/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"))
                .andReturn();

        byte[] zipBytes = result.getResponse().getContentAsByteArray();

        // Step 2: Extract ZIP
        Map<String, byte[]> extractedFiles = extractZipContents(zipBytes);

        // Step 3: Prüfen dass data.json existiert
        assertThat(extractedFiles).containsKey("data.json");

        // Step 4: Parse data.json
        ExportResult export = objectMapper.readValue(extractedFiles.get("data.json"), ExportResult.class);

        // Step 5: Validate ExportResult contents
        assertThat(export.getCategories()).isNotEmpty();
        assertThat(export.getRooms()).isNotEmpty();
        assertThat(export.getItems()).isNotEmpty();
        assertThat(export.getTags()).isNotEmpty();
        assertThat(export.getImages()).isNotEmpty();

        // Step 6: Prüfen ob zu jeder Image-UUID eine Datei im ZIP liegt
        for (ExportImage image : export.getImages()) {
            String imageFileName = "images/" + image.getUuid();
            assertThat(extractedFiles).containsKey(imageFileName);
        }
    }

    private Map<String, byte[]> extractZipContents(byte[] zipBytes) throws IOException {
        Map<String, byte[]> files = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                zis.transferTo(baos);
                files.put(entry.getName(), baos.toByteArray());
            }
        }

        return files;
    }
}

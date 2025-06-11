package de.iske.kistogramm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.ImportResult;
import de.iske.kistogramm.dto.export.ExportResult;
import de.iske.kistogramm.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private StorageRepository storageRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private CategoryAttributeTemplateRepository categoryAttributeTemplateRepository;

    @BeforeEach
    void setUp() {
        cleanup();
    }

    private void cleanup() {
        // Unlink all images from items to avoid foreign key constraint issues
        itemRepository.findAll().forEach(item -> {
            item.setImages(null);
            itemRepository.save(item);
        });
        // Unlink all images from storages to avoid foreign key constraint issues
        storageRepository.findAll().forEach(storage -> {
            storage.setImages(null);
            storageRepository.save(storage);
        });
        // Unlink all images from rooms to avoid foreign key constraint issues
        roomRepository.findAll().forEach(room -> {
            room.setImage(null);
            roomRepository.save(room);
        });

        // Clear all repositories before each test to ensure a clean state
        imageRepository.deleteAll();
        itemRepository.deleteAll();
        storageRepository.deleteAll();
        categoryAttributeTemplateRepository.deleteAll();
        categoryRepository.deleteAll();
        tagRepository.deleteAll();
        roomRepository.deleteAll();
    }

    @Test
    void shouldExportAndImportArchive() throws Exception {
        // create dataset similar to export test
        int wohnzimmerId = TestDataUtil.createRoomWithImage(mockMvc, objectMapper, "Wohnzimmer", "Raum mit Sofa");
        int schlafzimmerId = TestDataUtil.createRoomWithImage(mockMvc, objectMapper, "Schlafzimmer", "Mit Bett");
        int barId = TestDataUtil.createRoomWithImage(mockMvc, objectMapper, "Bar", "Raum mit Getränken");

        int regalId = TestDataUtil.createStorageWithImage(mockMvc, objectMapper, "Regal", "Für Bücher", wohnzimmerId);
        int schrankId = TestDataUtil.createStorageWithImage(mockMvc, objectMapper, "Schrank", "Kleidung", schlafzimmerId);
        int minibarId = TestDataUtil.createStorageWithImage(mockMvc, objectMapper, "Minibar", "Getränke", barId);
        int kisteId = TestDataUtil.createStorageWithImage(mockMvc, objectMapper, "Kiste", "Sonstiges", wohnzimmerId);

        int tagWerkzeug = TestDataUtil.createTag(mockMvc, objectMapper, "Werkzeug");
        int tagKueche = TestDataUtil.createTag(mockMvc, objectMapper, "Küche");
        int tagGarten = TestDataUtil.createTag(mockMvc, objectMapper, "Garten");
        int tagDeko = TestDataUtil.createTag(mockMvc, objectMapper, "Deko");
        int tagSport = TestDataUtil.createTag(mockMvc, objectMapper, "Sport");

        int kategorieBuchId = TestDataUtil.createCategoryWithTemplate(mockMvc, objectMapper, "Buch", List.of("Autor", "Verlag"));

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

        // export archive
        MvcResult exportRes = mockMvc.perform(get("/api/export"))
                .andExpect(status().isOk())
                .andReturn();
        byte[] zipBytes = exportRes.getResponse().getContentAsByteArray();
        Map<String, byte[]> extracted = extractZipContents(zipBytes);
        ExportResult exportResult = objectMapper.readValue(extracted.get("data.json"), ExportResult.class);

        // delete all data
        itemRepository.deleteAll();
        roomRepository.deleteAll();
        storageRepository.deleteAll();
        categoryAttributeTemplateRepository.deleteAll();
        categoryRepository.deleteAll();
        tagRepository.deleteAll();
        imageRepository.deleteAll();

        MockMultipartFile file = new MockMultipartFile("file", "export.zip", MediaType.APPLICATION_OCTET_STREAM_VALUE, zipBytes);
        String resp = mockMvc.perform(multipart("/api/import").file(file))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        ImportResult importResult = objectMapper.readValue(resp, ImportResult.class);
        assertThat(importResult.isSuccess()).isTrue();
        assertThat(itemRepository.count()).isEqualTo(exportResult.getItems().size());
        assertThat(categoryRepository.count()).isEqualTo(exportResult.getCategories().size());
        assertThat(tagRepository.count()).isEqualTo(exportResult.getTags().size());
        assertThat(roomRepository.count()).isEqualTo(exportResult.getRooms().size());
        assertThat(storageRepository.count()).isEqualTo(exportResult.getStorages().size());
        assertThat(imageRepository.count()).isEqualTo(exportResult.getImages().size());
    }

    @Test
    void shouldOverwriteExistingItemWhenEnabled() throws Exception {
        int roomId = TestDataUtil.createRoomWithImage(mockMvc, objectMapper, "Raum", "desc");
        int storageId = TestDataUtil.createStorageWithImage(mockMvc, objectMapper, "Box", "desc", roomId);
        int tagId = TestDataUtil.createTag(mockMvc, objectMapper, "Tag1");
        int catId = TestDataUtil.createCategoryWithTemplate(mockMvc, objectMapper, "Buch", List.of("Autor"));
        int itemId = TestDataUtil.createItemWithDetails(mockMvc, objectMapper, "BuchA", "desc", storageId, catId,
                Map.of("Autor", "A"), List.of(tagId), 1);

        MvcResult exportRes = mockMvc.perform(get("/api/export"))
                .andExpect(status().isOk())
                .andReturn();
        byte[] zipBytes = exportRes.getResponse().getContentAsByteArray();
        Map<String, byte[]> extracted = extractZipContents(zipBytes);
        ExportResult exportResult = objectMapper.readValue(extracted.get("data.json"), ExportResult.class);
        String originalName = exportResult.getItems().getFirst().getName();

        var entity = itemRepository.findById(itemId).orElseThrow();
        entity.setName("Changed");
        itemRepository.save(entity);

        MockMultipartFile file = new MockMultipartFile("file", "export.zip", MediaType.APPLICATION_OCTET_STREAM_VALUE, zipBytes);
        mockMvc.perform(multipart("/api/import").file(file).param("overwrite","false"))
                .andExpect(status().isOk());
        assertThat(itemRepository.findById(itemId).orElseThrow().getName()).isEqualTo("Changed");

        mockMvc.perform(multipart("/api/import").file(file).param("overwrite","true"))
                .andExpect(status().isOk());
        assertThat(itemRepository.findById(itemId).orElseThrow().getName()).isEqualTo(originalName);
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

    @Test
    void shouldReturnErrorWhenArchiveMissingDataJson() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("dummy.txt");
            zos.putNextEntry(entry);
            zos.write("test".getBytes());
            zos.closeEntry();
        }

        MockMultipartFile file = new MockMultipartFile("file", "nodata.zip", MediaType.APPLICATION_OCTET_STREAM_VALUE, baos.toByteArray());
        String resp = mockMvc.perform(multipart("/api/import").file(file))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        ImportResult result = objectMapper.readValue(resp, ImportResult.class);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrors()).contains("data.json missing in archive");
    }
}

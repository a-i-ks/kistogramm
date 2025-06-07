package de.iske.kistogramm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.ImportResult;
import de.iske.kistogramm.dto.export.ExportCategory;
import de.iske.kistogramm.dto.export.ExportCategoryAttributeTemplate;
import de.iske.kistogramm.dto.export.ExportResult;
import de.iske.kistogramm.model.CategoryAttributeTemplateEntity;
import de.iske.kistogramm.model.CategoryEntity;
import de.iske.kistogramm.repository.CategoryAttributeTemplateRepository;
import de.iske.kistogramm.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class ImportServiceTest {

    @Autowired
    private ImportService importService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryAttributeTemplateRepository templateRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSkipExistingTemplateWhenNoOverwrite() throws Exception {
        CategoryEntity cat = new CategoryEntity();
        cat.setName("Cat");
        cat.setDateAdded(LocalDateTime.now());
        cat.setDateModified(LocalDateTime.now());
        cat = categoryRepository.save(cat);

        CategoryAttributeTemplateEntity tpl = new CategoryAttributeTemplateEntity();
        tpl.setCategory(cat);
        tpl.setAttributeName("Color");
        tpl.setDateAdded(LocalDateTime.now());
        tpl.setDateModified(LocalDateTime.now());
        tpl = templateRepository.save(tpl);

        ExportCategory exportCategory = new ExportCategory();
        exportCategory.setUuid(cat.getUuid());
        exportCategory.setName(cat.getName());
        exportCategory.setDateAdded(cat.getDateAdded());
        exportCategory.setDateModified(cat.getDateModified());

        ExportCategoryAttributeTemplate exportTemplate = new ExportCategoryAttributeTemplate();
        exportTemplate.setUuid(tpl.getUuid());
        exportTemplate.setCategory(cat.getUuid());
        exportTemplate.setAttributeName("ColorUpdated");
        exportTemplate.setDateAdded(tpl.getDateAdded());
        exportTemplate.setDateModified(tpl.getDateModified());

        ExportResult exportResult = new ExportResult();
        exportResult.setCategories(List.of(exportCategory));
        exportResult.setCategoryAttributeTemplates(List.of(exportTemplate));

        MockMultipartFile file = createZip(exportResult);

        ImportResult result = importService.importArchive(file, false, true);

        CategoryAttributeTemplateEntity reloaded = templateRepository.findByUuid(tpl.getUuid()).orElseThrow();
        assertThat(reloaded.getAttributeName()).isEqualTo("Color");
        assertThat(result.getSkippedCategoryAttributeTemplateCount()).isEqualTo(1);
        assertThat(result.getUpdatedCategoryAttributeTemplateCount()).isZero();
    }

    @Test
    void shouldUpdateExistingTemplateWhenOverwrite() throws Exception {
        CategoryEntity cat = new CategoryEntity();
        cat.setName("Cat2");
        cat.setDateAdded(LocalDateTime.now());
        cat.setDateModified(LocalDateTime.now());
        cat = categoryRepository.save(cat);

        CategoryAttributeTemplateEntity tpl = new CategoryAttributeTemplateEntity();
        tpl.setCategory(cat);
        tpl.setAttributeName("Author");
        tpl.setDateAdded(LocalDateTime.now());
        tpl.setDateModified(LocalDateTime.now());
        tpl = templateRepository.save(tpl);

        ExportCategory exportCategory = new ExportCategory();
        exportCategory.setUuid(cat.getUuid());
        exportCategory.setName(cat.getName());
        exportCategory.setDateAdded(cat.getDateAdded());
        exportCategory.setDateModified(cat.getDateModified());

        ExportCategoryAttributeTemplate exportTemplate = new ExportCategoryAttributeTemplate();
        exportTemplate.setUuid(tpl.getUuid());
        exportTemplate.setCategory(cat.getUuid());
        exportTemplate.setAttributeName("Writer");
        exportTemplate.setDateAdded(tpl.getDateAdded());
        exportTemplate.setDateModified(tpl.getDateModified());

        ExportResult exportResult = new ExportResult();
        exportResult.setCategories(List.of(exportCategory));
        exportResult.setCategoryAttributeTemplates(List.of(exportTemplate));

        MockMultipartFile file = createZip(exportResult);

        ImportResult result = importService.importArchive(file, true, true);

        CategoryAttributeTemplateEntity reloaded = templateRepository.findByUuid(tpl.getUuid()).orElseThrow();
        assertThat(reloaded.getAttributeName()).isEqualTo("Writer");
        assertThat(result.getUpdatedCategoryAttributeTemplateCount()).isEqualTo(1);
    }

    @Test
    void shouldFailOnMissingCategoryWhenFailOnError() throws Exception {
        ExportCategoryAttributeTemplate template = new ExportCategoryAttributeTemplate();
        template.setUuid(UUID.randomUUID());
        template.setCategory(UUID.randomUUID());
        template.setAttributeName("Size");

        ExportResult result = new ExportResult();
        result.setCategoryAttributeTemplates(List.of(template));

        MockMultipartFile file = createZip(result);

        assertThatThrownBy(() -> importService.importArchive(file, true, true))
                .isInstanceOf(IllegalStateException.class);
    }

    private MockMultipartFile createZip(ExportResult exportResult) throws Exception {
        byte[] data = objectMapper.writeValueAsBytes(exportResult);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("data.json");
            zos.putNextEntry(entry);
            zos.write(data);
            zos.closeEntry();
        }
        return new MockMultipartFile("file", "import.zip", "application/zip", baos.toByteArray());
    }
}


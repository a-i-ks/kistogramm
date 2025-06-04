package de.iske.kistogramm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.export.*;
import de.iske.kistogramm.mapper.*;
import de.iske.kistogramm.model.ImageEntity;
import de.iske.kistogramm.repository.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportService {

    private final CategoryRepository categoryRepository;
    private final CategoryAttributeTemplateRepository categoryAttributeTemplateRepository;
    private final ImageRepository imageRepository;
    private final ItemRepository itemRepository;
    private final RoomRepository roomRepository;
    private final StorageRepository storageRepository;
    private final TagRepository tagRepository;

    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ItemMapper itemMapper;
    private final StorageMapper storageMapper;
    private final RoomMapper roomMapper;
    private final ImageMapper imageMapper;
    private final CategoryAttributeTemplateMapper categoryAttributeTemplateMapper;

    private final ObjectMapper objectMapper;

    public ExportService(
            CategoryRepository categoryRepository,
            CategoryAttributeTemplateRepository categoryAttributeTemplateRepository,
            ImageRepository imageRepository,
            ItemRepository itemRepository,
            RoomRepository roomRepository,
            StorageRepository storageRepository,
            TagRepository tagRepository,

            CategoryMapper categoryMapper,
            TagMapper tagMapper,
            ItemMapper itemMapper,
            StorageMapper storageMapper,
            RoomMapper roomMapper,
            ImageMapper imageMapper,
            CategoryAttributeTemplateMapper categoryAttributeTemplateMapper,

            ObjectMapper objectMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryAttributeTemplateRepository = categoryAttributeTemplateRepository;
        this.imageRepository = imageRepository;
        this.itemRepository = itemRepository;
        this.roomRepository = roomRepository;
        this.storageRepository = storageRepository;
        this.tagRepository = tagRepository;

        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.itemMapper = itemMapper;
        this.storageMapper = storageMapper;
        this.roomMapper = roomMapper;
        this.imageMapper = imageMapper;
        this.categoryAttributeTemplateMapper = categoryAttributeTemplateMapper;

        this.objectMapper = objectMapper;
    }

    public ByteArrayOutputStream exportToArchive() throws IOException {
        // Build the export result
        ExportResult exportResult = buildExportResult(List.of(ExportScope.ALL));
        // get all images
        List<ImageEntity> images = imageRepository.findAll().stream()
                .toList();

        var data = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportResult);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
            ZipEntry jsonEntry = new ZipEntry("data.json");
            zipOut.putNextEntry(jsonEntry);
            zipOut.write(data.getBytes());
            zipOut.closeEntry();

            for (var image : images) {
                String imagePath = "images/" + image.getUuid();
                ZipEntry imgEntry = new ZipEntry(imagePath);
                zipOut.putNextEntry(imgEntry);
                zipOut.write(image.getData());
                zipOut.closeEntry();
            }
        }

        return baos;
    }

    public ExportResult buildExportResult(List<ExportScope> exportScopes) {
        if (exportScopes == null || exportScopes.isEmpty() || exportScopes.contains(ExportScope.ALL)) {
            // Replace ALL with all available scopes
            exportScopes = List.of(
                    ExportScope.ITEMS,
                    ExportScope.CATEGORIES,
                    ExportScope.TAGS,
                    ExportScope.IMAGES,
                    ExportScope.STORAGES,
                    ExportScope.ROOMS,
                    ExportScope.CATEGORY_ATTRIBUTE_TEMPLATES
            );
        }
        ExportResult exportResult = new ExportResult();
        if (exportScopes.contains(ExportScope.ITEMS)) {
            exportResult.setItems(getExportItems());
        }
        if (exportScopes.contains(ExportScope.CATEGORIES)) {
            exportResult.setCategories(getExportCategories());
        }
        if (exportScopes.contains(ExportScope.TAGS)) {
            exportResult.setTags(getExportTags());
        }
        if (exportScopes.contains(ExportScope.IMAGES)) {
            exportResult.setImages(getExportImages());
        }
        if (exportScopes.contains(ExportScope.STORAGES)) {
            exportResult.setStorages(getExportStorages());
        }
        if (exportScopes.contains(ExportScope.ROOMS)) {
            exportResult.setRooms(getExportRooms());
        }
        if (exportScopes.contains(ExportScope.CATEGORY_ATTRIBUTE_TEMPLATES)) {
            exportResult.setCategoryAttributeTemplates(getCategoryAttributeTemplates());
        }
        return exportResult;
    }

    private List<ExportItem> getExportItems() {
        return itemRepository.findAll().stream()
                .map(itemMapper::toExportItem)
                .toList();
    }

    private List<ExportCategory> getExportCategories() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toExportCategory)
                .toList();
    }

    private List<ExportTag> getExportTags() {
        return tagRepository.findAll().stream()
                .map(tagMapper::toExportTag)
                .toList();
    }

    private List<ExportImage> getExportImages() {
        return imageRepository.findAll().stream()
                .map(imageMapper::toExportImage)
                .toList();
    }

    private List<ExportStorage> getExportStorages() {
        return storageRepository.findAll().stream()
                .map(storageMapper::toExportStorage)
                .toList();
    }

    private List<ExportRoom> getExportRooms() {
        return roomRepository.findAll().stream()
                .map(roomMapper::toExportRoom)
                .toList();
    }

    private List<ExportCategoryAttributeTemplate> getCategoryAttributeTemplates() {
        return categoryAttributeTemplateRepository.findAll().stream()
                .map(categoryAttributeTemplateMapper::toExportCategoryAttributeTemplate)
                .toList();
    }
}

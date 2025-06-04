package de.iske.kistogramm.dto.export;

import java.time.LocalDateTime;
import java.util.List;

public class ExportResult {
    List<ExportItem> items;
    List<ExportCategory> categories;
    List<ExportTag> tags;
    List<ExportImage> images;
    List<ExportStorage> storages;
    List<ExportRoom> rooms;
    List<ExportCategoryAttributeTemplate> categoryAttributeTemplates;
    private String version = "1.0";
    private LocalDateTime exportedAt = LocalDateTime.now();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(LocalDateTime exportedAt) {
        this.exportedAt = exportedAt;
    }

    public List<ExportItem> getItems() {
        return items;
    }

    public void setItems(List<ExportItem> items) {
        this.items = items;
    }

    public List<ExportCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<ExportCategory> categories) {
        this.categories = categories;
    }

    public List<ExportTag> getTags() {
        return tags;
    }

    public void setTags(List<ExportTag> tags) {
        this.tags = tags;
    }

    public List<ExportImage> getImages() {
        return images;
    }

    public void setImages(List<ExportImage> images) {
        this.images = images;
    }

    public List<ExportStorage> getStorages() {
        return storages;
    }

    public void setStorages(List<ExportStorage> storages) {
        this.storages = storages;
    }

    public List<ExportRoom> getRooms() {
        return rooms;
    }

    public void setRooms(List<ExportRoom> rooms) {
        this.rooms = rooms;
    }

    public List<ExportCategoryAttributeTemplate> getCategoryAttributeTemplates() {
        return categoryAttributeTemplates;
    }

    public void setCategoryAttributeTemplates(List<ExportCategoryAttributeTemplate> categoryAttributeTemplates) {
        this.categoryAttributeTemplates = categoryAttributeTemplates;
    }
}

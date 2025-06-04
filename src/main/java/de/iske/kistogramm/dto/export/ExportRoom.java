package de.iske.kistogramm.dto.export;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ExportRoom {

    private UUID uuid;
    private String name;
    private String description;
    private LocalDateTime dateAdded;
    private LocalDateTime dateModified;
    private List<UUID> storages;
    private UUID image;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    public LocalDateTime getDateModified() {
        return dateModified;
    }

    public void setDateModified(LocalDateTime dateModified) {
        this.dateModified = dateModified;
    }

    public List<UUID> getStorages() {
        return storages;
    }

    public void setStorages(List<UUID> storages) {
        this.storages = storages;
    }

    public UUID getImage() {
        return image;
    }

    public void setImage(UUID image) {
        this.image = image;
    }
}

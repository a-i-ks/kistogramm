package de.iske.kistogramm.dto.export;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ExportStorage {

    private UUID uuid;
    private String name;
    private String description;
    private UUID parentStorage;
    private UUID room;
    private List<UUID> images;
    private List<UUID> tags;
    private LocalDateTime dateAdded;
    private LocalDateTime dateModified;

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

    public UUID getParentStorage() {
        return parentStorage;
    }

    public void setParentStorage(UUID parentStorage) {
        this.parentStorage = parentStorage;
    }

    public UUID getRoom() {
        return room;
    }

    public void setRoom(UUID room) {
        this.room = room;
    }

    public List<UUID> getImages() {
        return images;
    }

    public void setImages(List<UUID> images) {
        this.images = images;
    }

    public List<UUID> getTags() {
        return tags;
    }

    public void setTags(List<UUID> tags) {
        this.tags = tags;
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
}

package de.iske.kistogramm.dto;

import com.google.common.base.MoreObjects;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Category {

    private Integer id;
    private UUID uuid;
    private String name;
    private String description;
    private LocalDateTime dateAdded;
    private LocalDateTime dateModified;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Category that)) return false;
        if (getUuid() != null && Objects.equals(getUuid(), that.getUuid())) return true;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getDateAdded(), that.getDateAdded())
                && Objects.equals(getDateModified(), that.getDateModified());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, uuid, name, dateAdded, dateModified);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("uuid", uuid)
                .add("name", name)
                .add("description", description)
                .add("dateAdded", dateAdded)
                .add("dateModified", dateModified)
                .toString();
    }
}

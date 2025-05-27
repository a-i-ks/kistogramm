package de.iske.kistogramm.dto;

import com.google.common.base.MoreObjects;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class Storage {

    private Integer id;
    private String name;
    private String description;
    private Integer roomId;
    private Integer parentStorageId;
    private List<Integer> tagIds;
    private LocalDate dateAdded;
    private LocalDate dateModified;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public Integer getParentStorageId() {
        return parentStorageId;
    }

    public void setParentStorageId(Integer parentStorageId) {
        this.parentStorageId = parentStorageId;
    }

    public List<Integer> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Integer> tagIds) {
        this.tagIds = tagIds;
    }

    public LocalDate getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDate dateAdded) {
        this.dateAdded = dateAdded;
    }

    public LocalDate getDateModified() {
        return dateModified;
    }

    public void setDateModified(LocalDate dateModified) {
        this.dateModified = dateModified;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Storage that)) return false;
        if (getId() != null && Objects.equals(getId(), that.getId())) return true;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getDescription(), that.getDescription())
                && Objects.equals(getRoomId(), that.getRoomId())
                && Objects.equals(getParentStorageId(), that.getParentStorageId())
                && Objects.equals(getTagIds(), that.getTagIds())
                && Objects.equals(getDateAdded(), that.getDateAdded())
                && Objects.equals(getDateModified(), that.getDateModified());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, roomId, parentStorageId, tagIds, dateAdded, dateModified);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("description", description)
                .add("roomId", roomId)
                .add("parentStorageId", parentStorageId)
                .add("tagIds", tagIds)
                .add("dateAdded", dateAdded)
                .add("dateModified", dateModified)
                .toString();
    }
}

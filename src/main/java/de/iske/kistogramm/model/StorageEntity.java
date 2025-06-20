package de.iske.kistogramm.model;

import com.google.common.base.MoreObjects;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "storages")
public class StorageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    private String name;
    private String description;

    @ManyToOne
    private RoomEntity room;

    @ManyToOne
    private StorageEntity parentStorage;

    @OneToMany(mappedBy = "parentStorage")
    private Set<StorageEntity> subStorages = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "storage_tags",
            joinColumns = @JoinColumn(name = "storage_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<TagEntity> tags = new HashSet<>();

    @OneToMany(mappedBy = "storage", cascade = CascadeType.ALL)
    private Set<ItemEntity> items = new HashSet<>();

    @OneToMany(mappedBy = "storage", cascade = CascadeType.ALL)
    private Set<ImageEntity> images = new HashSet<>();

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

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RoomEntity getRoom() {
        return room;
    }

    public void setRoom(RoomEntity room) {
        this.room = room;
    }

    public StorageEntity getParentStorage() {
        return parentStorage;
    }

    public void setParentStorage(StorageEntity parentStorage) {
        this.parentStorage = parentStorage;
    }

    public Set<StorageEntity> getSubStorages() {
        return subStorages;
    }

    public void setSubStorages(Set<StorageEntity> subStorages) {
        if (this.subStorages.equals(subStorages)) {
            return;
        }
        this.subStorages.clear();
        if (subStorages != null) {
            this.subStorages.addAll(subStorages);
        }
        this.subStorages.forEach(s -> s.setParentStorage(this));
    }

    public Set<TagEntity> getTags() {
        return tags;
    }

    public void setTags(Set<TagEntity> tags) {
        if (this.tags.equals(tags)) {
            return;
        }
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
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

    public Set<ItemEntity> getItems() {
        return items;
    }

    public void setItems(Set<ItemEntity> items) {
        if (this.items.equals(items)) {
            return;
        }
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
        this.items.forEach(i -> i.setStorage(this));
    }

    public Set<ImageEntity> getImages() {
        return images;
    }

    public void setImages(Set<ImageEntity> images) {
        if (this.images.equals(images)) {
            return;
        }
        this.images.clear();
        if (images != null) {
            this.images.addAll(images);
        }
        this.images.forEach(i -> i.setStorage(this));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StorageEntity that)) return false;
        return getUuid() != null && Objects.equals(getUuid(), that.getUuid());
    }

    @Override
    public int hashCode() {
        return 42;
    }

    @PrePersist
    public void ensureUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID(); // Ensure UUID is generated before persisting
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("uuid", uuid)
                .add("name", name)
                .add("numberOfItems", items.size())
                .add("description", description)
                .add("room", room)
                .add("parentStorageId", parentStorage != null ? parentStorage.getId() : null)
                .add("subStoragesIds", getIdsOfSubStorages())
                .add("tags", getTagsAsString())
                .add("imagesIds", getImagesIds())
                .add("dateAdded", dateAdded)
                .add("dateModified", dateModified)
                .toString();
    }

    private String getImagesIds() {
        if (images == null || images.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (ImageEntity image : images) {
            sb.append(image.getId()).append(", ");
        }
        sb.setLength(sb.length() - 2); // Remove last comma and space
        sb.append("]");
        return sb.toString();
    }

    private String getTagsAsString() {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (TagEntity tag : tags) {
            sb.append(tag.getId()).append(", ");
        }
        sb.setLength(sb.length() - 2); // Remove last comma and space
        sb.append("]");
        return sb.toString();
    }

    private String getIdsOfSubStorages() {
        if (subStorages == null || subStorages.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (StorageEntity subStorage : subStorages) {
            sb.append(subStorage.getId()).append(", ");
        }
        sb.setLength(sb.length() - 2); // Remove last comma and space
        sb.append("]");
        return sb.toString();
    }
}

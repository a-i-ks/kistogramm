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

    @OneToMany(mappedBy = "storage", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ItemEntity> items = new HashSet<>();

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
        this.subStorages = subStorages;
    }

    public Set<TagEntity> getTags() {
        return tags;
    }

    public void setTags(Set<TagEntity> tags) {
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

    public Set<ItemEntity> getItems() {
        return items;
    }

    public void setItems(Set<ItemEntity> items) {
        this.items = items;
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
                .add("parentStorage", parentStorage)
                .add("dateAdded", dateAdded)
                .add("dateModified", dateModified)
                .toString();
    }
}

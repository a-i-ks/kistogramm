package de.iske.kistogramm.model;

import com.google.common.base.MoreObjects;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "rooms")
public class RoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    private String name;
    private String description;

    private LocalDateTime dateAdded;
    private LocalDateTime dateModified;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    private Set<StorageEntity> storages = new HashSet<>();

    @ManyToOne
    private ImageEntity image;

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

    public Set<StorageEntity> getStorages() {
        return storages;
    }

    public void setStorages(Set<StorageEntity> storages) {
        this.storages = storages;
    }

    public ImageEntity getImage() {
        return image;
    }

    public void setImage(ImageEntity image) {
        this.image = image;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RoomEntity that)) return false;
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
                .add("imageId", image != null ? image.getId() : null)
                .add("storageIds", getStoragesIds())
                .add("description", description)
                .add("dateAdded", dateAdded)
                .add("dateModified", dateModified)
                .toString();
    }

    private String getStoragesIds() {
        if (storages == null || storages.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (StorageEntity storage : storages) {
            sb.append(storage.getId()).append(", ");
        }
        sb.setLength(sb.length() - 2); // Remove last comma and space
        sb.append("]");
        return sb.toString();
    }
}

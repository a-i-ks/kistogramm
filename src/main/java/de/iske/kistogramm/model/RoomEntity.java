package de.iske.kistogramm.model;

import com.google.common.base.MoreObjects;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
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
                .add("description", description)
                .add("dateAdded", dateAdded)
                .add("dateModified", dateModified)
                .toString();
    }
}

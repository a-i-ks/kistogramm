package de.iske.kistogramm.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "images")
public class ImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "data", nullable = false, columnDefinition = "BYTEA")
    private byte[] data;

    private LocalDateTime dateAdded;
    private LocalDateTime dateModified;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private ItemEntity item;

    @ManyToOne
    @JoinColumn(name = "storage_id")
    private StorageEntity storage;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private RoomEntity room;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
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

    public ItemEntity getItem() {
        return item;
    }

    public void setItem(ItemEntity item) {
        this.item = item;
    }

    public StorageEntity getStorage() {
        return storage;
    }

    public void setStorage(StorageEntity storage) {
        this.storage = storage;
    }

    public RoomEntity getRoom() {
        return room;
    }

    public void setRoom(RoomEntity room) {
        this.room = room;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ImageEntity that)) return false;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return 42;
    }

    private String resolveOwner() {
        if (item != null) {
            return "Item[id=" + item.getId() + "]";
        } else if (storage != null) {
            return "Storage[id=" + storage.getId() + "]";
        } else if (room != null) {
            return "Room[id=" + room.getId() + "]";
        } else {
            return "Unassigned";
        }
    }

    @Override
    public String toString() {
        return com.google.common.base.MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("belongsTo", resolveOwner())
                .add("data.length", data != null ? data.length : 0)
                .add("dateAdded", dateAdded)
                .add("dateModified", dateModified)
                .toString();
    }
}

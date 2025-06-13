package de.iske.kistogramm.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "images")
public class ImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    private String description;

    @Column(name = "type")
    private String type;

    @Column(name = "data", nullable = false, columnDefinition = "BYTEA")
    private byte[] data;

    private LocalDateTime dateAdded;
    private LocalDateTime dateModified;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private ItemEntity item;

    @ManyToOne
    @JoinColumn(name = "receipt_item_id")
    private ItemEntity receiptItem;

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

    public UUID getUuid() {
        return uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
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

    public ItemEntity getReceiptItem() {
        return receiptItem;
    }

    public void setReceiptItem(ItemEntity receiptItem) {
        this.receiptItem = receiptItem;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ImageEntity that)) return false;
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

    private String resolveOwner() {
        if (item != null) {
            return "Item[id=" + item.getId() + "]";
        } else if (receiptItem != null) {
            return "Receipt of Item[id=" + receiptItem.getId() + "]";
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
                .add("uuid", uuid)
                .add("description", description)
                .add("type", type)
                .add("belongsTo", resolveOwner())
                .add("data.length", data != null ? data.length : 0)
                .add("dateAdded", dateAdded)
                .add("dateModified", dateModified)
                .toString();
    }
}

package de.iske.kistogramm.dto;

import com.google.common.base.MoreObjects;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class Image {

    private Integer id;
    private UUID uuid;
    private byte[] data;
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

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Image that)) return false;
        if (getUuid() != null && Objects.equals(getUuid(), that.getUuid())) return true;
        return Arrays.equals(getData(), that.getData()) &&
                Objects.equals(getDateAdded(), that.getDateAdded()) &&
                Objects.equals(getDateModified(), that.getDateModified());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, dateAdded, dateModified);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("uuid", uuid)
                .add("data.length", data != null ? data.length : 0)
                .add("dateAdded", dateAdded)
                .add("dateModified", dateModified)
                .toString();
    }
}

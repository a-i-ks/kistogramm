package de.iske.kistogramm.dto;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

public class Image {

    private Integer id;
    private byte[] data;
    private LocalDate dateAdded;
    private LocalDate dateModified;

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
        if (!(obj instanceof Image that)) return false;
        if (getId() != null && Objects.equals(getId(), that.getId())) return true;
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
        return com.google.common.base.MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("data.length", data != null ? data.length : 0)
                .add("dateAdded", dateAdded)
                .add("dateModified", dateModified)
                .toString();
    }
}

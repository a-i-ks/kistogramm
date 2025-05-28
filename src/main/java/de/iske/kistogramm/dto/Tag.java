package de.iske.kistogramm.dto;

import com.google.common.base.MoreObjects;

import java.time.LocalDateTime;
import java.util.Objects;

public class Tag {

    private Integer id;
    private String name;
    private LocalDateTime dateAdded;
    private LocalDateTime dateModified;

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
        if (!(obj instanceof Tag that)) return false;
        if (getId() != null && Objects.equals(getId(), that.getId())) return true;
        return Objects.equals(getName(), that.getName()) &&
                Objects.equals(getDateAdded(), that.getDateAdded()) &&
                Objects.equals(getDateModified(), that.getDateModified());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, dateAdded, dateModified);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("dateAdded", dateAdded)
                .add("dateModified", dateModified)
                .toString();
    }
}

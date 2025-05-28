package de.iske.kistogramm.dto;

import java.time.LocalDateTime;
import java.util.Objects;

public class CategoryAttributeTemplate {

    private Integer id;
    private Integer categoryId;
    private String attributeName;
    private LocalDateTime dateAdded;
    private LocalDateTime dateModified;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
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
        if (!(obj instanceof CategoryAttributeTemplate that)) return false;
        if (getId() != null && Objects.equals(getId(), that.getId())) return true;
        return Objects.equals(getCategoryId(), that.getCategoryId()) &&
                Objects.equals(getAttributeName(), that.getAttributeName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, categoryId, attributeName);
    }

    @Override
    public String toString() {
        return com.google.common.base.MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("categoryId", categoryId)
                .add("attributeName", attributeName)
                .add("dateAdded", dateAdded)
                .add("dateModified", dateModified)
                .toString();
    }
}

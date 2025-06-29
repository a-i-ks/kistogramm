package de.iske.kistogramm.dto;

import com.google.common.base.MoreObjects;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class Item {

    private Integer id;
    private UUID uuid;
    private String name;
    private String description;
    private LocalDate purchaseDate;
    private Double purchasePrice;
    private Integer quantity;
    private LocalDateTime dateAdded;
    private LocalDateTime dateModified;

    private Integer categoryId;
    private Integer storageId;
    private Set<Integer> tagIds = new HashSet<>();
    private Set<Integer> relatedItemIds = new HashSet<>();
    private Set<Integer> imageIds = new HashSet<>();
    private Set<Integer> receiptIds = new HashSet<>();

    private Map<String, String> customAttributes = new HashMap<>();

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

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(Double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getStorageId() {
        return storageId;
    }

    public void setStorageId(Integer storageId) {
        this.storageId = storageId;
    }

    public Set<Integer> getTagIds() {
        return tagIds;
    }

    public void setTagIds(Set<Integer> tagIds) {
        this.tagIds = tagIds;
    }

    public Set<Integer> getRelatedItemIds() {
        return relatedItemIds;
    }

    public void setRelatedItemIds(Set<Integer> relatedItemIds) {
        this.relatedItemIds = relatedItemIds;
    }

    public Set<Integer> getImageIds() {
        return imageIds;
    }

    public void setImageIds(Set<Integer> imageIds) {
        this.imageIds = imageIds;
    }

    public Set<Integer> getReceiptIds() {
        return receiptIds;
    }

    public void setReceiptIds(Set<Integer> receiptIds) {
        this.receiptIds = receiptIds;
    }

    public Map<String, String> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, String> customAttributes) {
        this.customAttributes = customAttributes;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Item that)) return false;
        if (getUuid() != null && Objects.equals(getUuid(), that.getUuid())) return true;
        return Objects.equals(getName(), that.getName()) &&
                Objects.equals(getDescription(), that.getDescription()) &&
                Objects.equals(getPurchaseDate(), that.getPurchaseDate()) &&
                Objects.equals(getPurchasePrice(), that.getPurchasePrice()) &&
                Objects.equals(getQuantity(), that.getQuantity());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, uuid, name, description, purchaseDate, purchasePrice, quantity);
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper stringHelper = MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("uuid", uuid)
                .add("name", name)
                .add("description", description)
                .add("purchaseDate", purchaseDate)
                .add("purchasePrice", purchasePrice)
                .add("quantity", quantity);
        // add custom attributes if they exist
        if (customAttributes != null && !customAttributes.isEmpty()) {
            customAttributes.keySet().forEach(key -> {
                String value = customAttributes.get(key);
                if (value != null && !value.isEmpty()) {
                    stringHelper.add(key, value);
                }
            });
        }
        return stringHelper.toString();
    }
}

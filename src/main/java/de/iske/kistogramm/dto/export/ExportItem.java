package de.iske.kistogramm.dto.export;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExportItem {

    private UUID uuid;
    private String name;
    private String description;
    private LocalDate purchaseDate;
    private Double purchasePrice;
    private Integer quantity;
    private LocalDateTime dateAdded;
    private LocalDateTime dateModified;
    private List<UUID> images;
    private List<UUID> receipts;
    private UUID storage;
    private List<UUID> tags;
    private UUID category;
    private List<UUID> relatedItems;
    private Map<String, String> customAttributes;

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

    public List<UUID> getImages() {
        return images;
    }

    public void setImages(List<UUID> images) {
        this.images = images;
    }

    public List<UUID> getReceipts() {
        return receipts;
    }

    public void setReceipts(List<UUID> receipts) {
        this.receipts = receipts;
    }

    public UUID getStorage() {
        return storage;
    }

    public void setStorage(UUID storage) {
        this.storage = storage;
    }

    public List<UUID> getTags() {
        return tags;
    }

    public void setTags(List<UUID> tags) {
        this.tags = tags;
    }

    public UUID getCategory() {
        return category;
    }

    public void setCategory(UUID category) {
        this.category = category;
    }

    public List<UUID> getRelatedItems() {
        return relatedItems;
    }

    public void setRelatedItems(List<UUID> relatedItems) {
        this.relatedItems = relatedItems;
    }

    public Map<String, String> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, String> customAttributes) {
        this.customAttributes = customAttributes;
    }
}
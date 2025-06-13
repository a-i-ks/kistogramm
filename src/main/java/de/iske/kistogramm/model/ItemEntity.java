package de.iske.kistogramm.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "items")
public class ItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    private String name;
    private String description;

    private LocalDate purchaseDate;
    private Double purchasePrice;
    private Integer quantity;

    private LocalDateTime dateAdded;
    private LocalDateTime dateModified;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @ManyToOne
    @JoinColumn(name = "storage_id")
    private StorageEntity storage;

    @ManyToMany
    @JoinTable(
            name = "item_tags",
            joinColumns = @JoinColumn(name = "item_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<TagEntity> tags = new HashSet<>();

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    private Set<ImageEntity> images = new HashSet<>();

    @OneToMany(mappedBy = "receiptItem", cascade = CascadeType.ALL)
    private Set<ImageEntity> receipts = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "item_related",
            joinColumns = @JoinColumn(name = "item_id"),
            inverseJoinColumns = @JoinColumn(name = "related_item_id")
    )
    private Set<ItemEntity> relatedItems = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "item_attributes", joinColumns = @JoinColumn(name = "item_id"))
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value")
    private Map<String, String> customAttributes = new HashMap<>();

    // Getter & Setter
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

    public CategoryEntity getCategory() {
        return category;
    }

    public void setCategory(CategoryEntity category) {
        this.category = category;
    }

    public StorageEntity getStorage() {
        return storage;
    }

    public void setStorage(StorageEntity storage) {
        this.storage = storage;
    }

    public Set<ItemEntity> getRelatedItems() {
        return relatedItems;
    }

    public void setRelatedItems(Set<ItemEntity> relatedItems) {
        if (this.relatedItems.equals(relatedItems)) {
            return;
        }
        this.relatedItems.clear();
        if (relatedItems != null) {
            this.relatedItems.addAll(relatedItems);
        }
        this.relatedItems.forEach(i -> i.getRelatedItems().add(this)); // Ensure bidirectional relationship
    }

    public Set<TagEntity> getTags() {
        return tags;
    }

    public void setTags(Set<TagEntity> tags) {
        if (this.tags.equals(tags)) {
            return;
        }
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    public Set<ImageEntity> getImages() {
        return images;
    }

    public void setImages(Set<ImageEntity> images) {
        if (this.images.equals(images)) {
            return;
        }
        this.images.clear();
        if (images != null) {
            this.images.addAll(images);
        }
        this.images.forEach(i -> i.setItem(this));
    }

    public Set<ImageEntity> getReceipts() {
        return receipts;
    }

    public void setReceipts(Set<ImageEntity> receipts) {
        if (this.receipts.equals(receipts)) {
            return;
        }
        this.receipts.clear();
        if (receipts != null) {
            this.receipts.addAll(receipts);
        }
        this.receipts.forEach(i -> i.setItem(this));
    }

    public Map<String, String> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, String> customAttributes) {
        if (this.customAttributes.equals(customAttributes)) {
            return;
        }
        this.customAttributes.clear();
        if (customAttributes != null) {
            this.customAttributes.putAll(customAttributes);
        }
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

    // equals & hashCode
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ItemEntity that)) {
            return false;
        }
        return getUuid() != null && Objects.equals(getUuid(), that.getUuid());
    }

    @Override
    public int hashCode() {
        return 42; // id is DB-generated
    }

    @PrePersist
    public void ensureUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID(); // Ensure UUID is generated before persisting
        }
    }

    // toString
    @Override
    public String toString() {
        return toStringHelper().toString();
    }

    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("uuid", uuid)
                .add("name", name)
                .add("description", description)
                .add("purchaseDate", purchaseDate)
                .add("purchasePrice", purchasePrice)
                .add("quantity", quantity)
                .add("category", category != null ? category.getId() : null)
                .add("storage", storage != null ? storage.getId() : null)
                .add("relatedItems", relatedItems.size())
                .add("tags", tags.size())
                .add("images", images.size())
                .add("receipts", receipts.size())
                .add("customAttributes", customAttributes)
                .add("dateAdded", dateAdded)
                .add("dateModified", dateModified);
    }
}

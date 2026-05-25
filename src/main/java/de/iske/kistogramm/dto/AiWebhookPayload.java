package de.iske.kistogramm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class AiWebhookPayload {

    @NotNull
    private UUID jobId;

    @NotBlank
    private String name;

    private String description;
    private String category;
    private List<String> tags;
    private Integer quantity;
    private Double purchasePrice;

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(Double purchasePrice) { this.purchasePrice = purchasePrice; }
}

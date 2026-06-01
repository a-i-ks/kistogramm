package de.iske.kistogramm.controller;

import de.iske.kistogramm.dto.AiJobResponse;
import de.iske.kistogramm.dto.Image;
import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.model.AiJobEntity;
import de.iske.kistogramm.service.AiQueueService;
import de.iske.kistogramm.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;
    private final AiQueueService aiQueueService;

    public ItemController(ItemService itemService, AiQueueService aiQueueService) {
        this.itemService = itemService;
        this.aiQueueService = aiQueueService;
    }

    public static class AnalyzeRequest {
        private String jobType;
        public String getJobType() { return jobType; }
        public void setJobType(String jobType) { this.jobType = jobType; }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Item> getAllItems() {
        return itemService.getAllItems();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Item> getItemById(@PathVariable Integer id) {
        return itemService.getItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Item> createItem(@RequestBody Item item) {
        return ResponseEntity.ok(itemService.createItem(item));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Item> updateItem(@PathVariable Integer id, @RequestBody Item item) {
        Item updated = itemService.updateItem(id, item);
        return ResponseEntity.ok(updated);
    }


    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteItem(@PathVariable Integer id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/related")
    @Transactional
    public ResponseEntity<Item> linkRelatedItems(@PathVariable Integer id,
                                                 @RequestBody List<Integer> relatedItemIds) {
        Item updated = itemService.linkRelatedItems(id, relatedItemIds);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/tags")
    @Transactional
    public ResponseEntity<Item> updateTags(@PathVariable Integer id, @RequestBody List<Integer> tagIds) {
        Item updated = itemService.updateTags(id, tagIds);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/images")
    @Transactional
    public ResponseEntity<Item> uploadImages(@PathVariable Integer id,
                                             @RequestParam("files") List<MultipartFile> files) {
        var updatedItem = itemService.uploadImages(id, files);
        return ResponseEntity.ok(updatedItem);
    }

    @PostMapping("/{id}/receipts")
    @Transactional
    public ResponseEntity<Item> uploadReceipts(@PathVariable Integer id,
                                               @RequestParam("files") List<MultipartFile> files) {
        var updatedItem = itemService.uploadReceipts(id, files);
        return ResponseEntity.ok(updatedItem);
    }

    @GetMapping("/{id}/images")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Image>> getImagesByItemId(@PathVariable Integer id) {
        List<Image> imageIds = itemService.getImageIdsByItemId(id);
        return ResponseEntity.ok(imageIds);
    }

    @GetMapping("/{id}/receipts")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Image>> getReceiptsByItemId(@PathVariable Integer id) {
        List<Image> receiptIds = itemService.getReceiptIdsByItemId(id);
        return ResponseEntity.ok(receiptIds);
    }

    @DeleteMapping("/{itemId}/images")
    @Transactional
    public ResponseEntity<Void> deleteAllImagesFromItem(@PathVariable Integer itemId) {
        itemService.deleteAllImagesFromItem(itemId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{itemId}/receipts")
    @Transactional
    public ResponseEntity<Void> deleteAllReceiptsFromItem(@PathVariable Integer itemId) {
        itemService.deleteAllReceiptsFromItem(itemId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{itemId}/images/{imageId}")
    @Transactional
    public ResponseEntity<Void> deleteItemImage(@PathVariable Integer itemId, @PathVariable Integer imageId) {
        itemService.deleteImageFromItem(itemId, imageId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{itemId}/receipts/{receiptId}")
    @Transactional
    public ResponseEntity<Void> deleteItemReceipt(@PathVariable Integer itemId, @PathVariable Integer receiptId) {
        itemService.deleteReceiptFromItem(itemId, receiptId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/ai/analyze")
    @Transactional
    public ResponseEntity<AiJobResponse> analyzeItem(
            @PathVariable Integer id,
            @RequestBody AnalyzeRequest request) throws IOException {
        AiJobEntity.JobType jobType;
        try {
            jobType = AiJobEntity.JobType.valueOf(request.getJobType());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown or missing jobType: " + request.getJobType());
        }
        if (jobType == AiJobEntity.JobType.INGESTION) {
            throw new IllegalArgumentException("INGESTION jobs cannot be triggered via this endpoint.");
        }
        AiJobEntity job = aiQueueService.submitAnalysisJob(id, jobType);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(AiJobResponse.from(job));
    }
}

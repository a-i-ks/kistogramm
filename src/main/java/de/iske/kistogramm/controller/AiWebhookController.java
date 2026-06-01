package de.iske.kistogramm.controller;

import de.iske.kistogramm.dto.AiWebhookPayload;
import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.model.AiJobEntity;
import de.iske.kistogramm.model.ImageEntity;
import de.iske.kistogramm.model.TagEntity;
import de.iske.kistogramm.repository.AiJobRepository;
import de.iske.kistogramm.repository.CategoryRepository;
import de.iske.kistogramm.repository.ImageRepository;
import de.iske.kistogramm.repository.ItemRepository;
import de.iske.kistogramm.repository.TagRepository;
import de.iske.kistogramm.service.ImageCompressionService;
import de.iske.kistogramm.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/ai/webhook")
public class AiWebhookController {

    @Value("${ai.webhook-secret}")
    private String webhookSecret;

    private final AiJobRepository aiJobRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ImageRepository imageRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;
    private final ImageCompressionService imageCompressionService;

    public AiWebhookController(AiJobRepository aiJobRepository,
                                CategoryRepository categoryRepository,
                                TagRepository tagRepository,
                                ImageRepository imageRepository,
                                ItemRepository itemRepository,
                                ItemService itemService,
                                ImageCompressionService imageCompressionService) {
        this.aiJobRepository = aiJobRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.imageRepository = imageRepository;
        this.itemRepository = itemRepository;
        this.itemService = itemService;
        this.imageCompressionService = imageCompressionService;
    }

    @PostMapping("/result")
    @Transactional
    public ResponseEntity<Void> handleResult(
            @RequestHeader("X-Webhook-Secret") String secret,
            @Valid @RequestBody AiWebhookPayload payload) {

        if (!webhookSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        AiJobEntity job = aiJobRepository.findById(payload.getJobId()).orElse(null);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (job.getStatus() == AiJobEntity.Status.CANCELLED) {
            return ResponseEntity.ok().build();
        }

        job.setStatus(AiJobEntity.Status.PROCESSING);
        aiJobRepository.save(job);

        try {
            if (job.getJobType() == AiJobEntity.JobType.INGESTION) {
                handleIngestionResult(job, payload);
            } else {
                handleAnalysisResult(job, payload);
            }
        } catch (Exception e) {
            job.setStatus(AiJobEntity.Status.FAILED);
            job.setErrorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        aiJobRepository.save(job);
        return ResponseEntity.ok().build();
    }

    private void handleIngestionResult(AiJobEntity job, AiWebhookPayload payload) {
        Item dto = new Item();
        dto.setName(payload.getName());
        dto.setDescription(payload.getDescription());
        dto.setQuantity(payload.getQuantity() != null ? payload.getQuantity() : 1);
        dto.setPurchasePrice(payload.getPurchasePrice());
        dto.setStorageId(job.getStorageId());

        if (payload.getCategory() != null && !payload.getCategory().isBlank()) {
            categoryRepository.findByName(payload.getCategory())
                    .ifPresent(c -> dto.setCategoryId(c.getId()));
        }

        if (payload.getTags() != null && !payload.getTags().isEmpty()) {
            Set<Integer> tagIds = new HashSet<>();
            for (String tagName : payload.getTags()) {
                if (tagName == null || tagName.isBlank()) continue;
                TagEntity tag = tagRepository.findByName(tagName).orElseGet(() -> {
                    TagEntity t = new TagEntity();
                    t.setName(tagName);
                    t.setDateAdded(LocalDateTime.now());
                    t.setDateModified(LocalDateTime.now());
                    return tagRepository.save(t);
                });
                tagIds.add(tag.getId());
            }
            dto.setTagIds(tagIds);
        }

        Item created = itemService.createItem(dto);
        attachCapturedImage(job.getImagePath(), created.getId());

        job.setStatus(AiJobEntity.Status.DONE);
        job.setItemId(created.getId());
    }

    private void handleAnalysisResult(AiJobEntity job, AiWebhookPayload payload) {
        String proposalData = payload.getProposalData();
        if (proposalData == null || proposalData.isBlank()) {
            throw new IllegalArgumentException(
                    "Worker returned empty proposal data for job type " + job.getJobType());
        }
        job.setProposalData(proposalData);
        job.setProposalStatus(AiJobEntity.ProposalStatus.PENDING_REVIEW);
        job.setStatus(AiJobEntity.Status.DONE);
    }

    private void attachCapturedImage(String imagePath, Integer itemId) {
        if (imagePath == null || itemId == null) return;
        try {
            Path path = Path.of(imagePath);
            if (!Files.exists(path)) return;

            byte[] raw = Files.readAllBytes(path);
            String mimeType = detectMimeType(imagePath);
            byte[] data = imageCompressionService.compress(raw, mimeType);

            var item = itemRepository.findById(itemId).orElse(null);
            if (item == null) return;

            ImageEntity image = new ImageEntity();
            image.setData(data);
            image.setType(mimeType);
            image.setDateAdded(LocalDateTime.now());
            image.setDateModified(LocalDateTime.now());
            image.setItem(item);
            imageRepository.save(image);
        } catch (Exception e) {
            // Non-fatal: item was created, image attachment failed
        }
    }

    private String detectMimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
}

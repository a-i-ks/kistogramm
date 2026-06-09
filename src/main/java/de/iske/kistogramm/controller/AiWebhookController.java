package de.iske.kistogramm.controller;

import de.iske.kistogramm.dto.AiWebhookPayload;
import de.iske.kistogramm.dto.AppSettingsDto;
import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.model.AiJobEntity;
import de.iske.kistogramm.model.ImageEntity;
import de.iske.kistogramm.model.TagEntity;
import de.iske.kistogramm.repository.AiJobRepository;
import de.iske.kistogramm.repository.CategoryRepository;
import de.iske.kistogramm.repository.ImageRepository;
import de.iske.kistogramm.repository.ItemRepository;
import de.iske.kistogramm.repository.TagRepository;
import de.iske.kistogramm.service.AppSettingsService;
import de.iske.kistogramm.service.ImageCompressionService;
import de.iske.kistogramm.service.ItemService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AiWebhookController.class);

    @Value("${ai.webhook-secret}")
    private String webhookSecret;

    private final AiJobRepository aiJobRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ImageRepository imageRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;
    private final ImageCompressionService imageCompressionService;
    private final AppSettingsService appSettingsService;

    public AiWebhookController(AiJobRepository aiJobRepository,
                                CategoryRepository categoryRepository,
                                TagRepository tagRepository,
                                ImageRepository imageRepository,
                                ItemRepository itemRepository,
                                ItemService itemService,
                                ImageCompressionService imageCompressionService,
                                AppSettingsService appSettingsService) {
        this.aiJobRepository = aiJobRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.imageRepository = imageRepository;
        this.itemRepository = itemRepository;
        this.itemService = itemService;
        this.imageCompressionService = imageCompressionService;
        this.appSettingsService = appSettingsService;
    }

    @PostMapping("/result")
    @Transactional
    public ResponseEntity<Void> handleResult(
            @RequestHeader("X-Webhook-Secret") String secret,
            @Valid @RequestBody AiWebhookPayload payload) {

        if (!webhookSecret.equals(secret)) {
            log.warn("Webhook rejected: invalid secret");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Webhook received: jobId={} type={}", payload.getJobId(), payload.getJobType());

        AiJobEntity job = aiJobRepository.findById(payload.getJobId()).orElse(null);
        if (job == null) {
            log.warn("Webhook: job not found: {}", payload.getJobId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (job.getStatus() == AiJobEntity.Status.CANCELLED) {
            log.info("Webhook: job {} is CANCELLED, ignoring result", payload.getJobId());
            return ResponseEntity.ok().build();
        }

        if (payload.getError() != null && !payload.getError().isBlank()) {
            log.error("Webhook: worker reported error for jobId={} type={}: {}",
                    payload.getJobId(), job.getJobType(), payload.getError());
            if (isRetryableError(payload.getError())) {
                AppSettingsDto settings = appSettingsService.getSettings();
                if (settings.isAiRetryEnabled() && job.getRetryCount() < settings.getAiRetryMaxAttempts()) {
                    job.setRetryCount(job.getRetryCount() + 1);
                    job.setNextRetryAt(LocalDateTime.now().plusSeconds(settings.getAiRetryDelaySeconds()));
                    job.setStatus(AiJobEntity.Status.AWAITING_RETRY);
                    job.setErrorMessage(payload.getError());
                    aiJobRepository.save(job);
                    log.warn("Webhook: job {} scheduled for retry {}/{} in {}s",
                            job.getId(), job.getRetryCount(), settings.getAiRetryMaxAttempts(),
                            settings.getAiRetryDelaySeconds());
                    return ResponseEntity.ok().build();
                }
            }
            job.setStatus(AiJobEntity.Status.FAILED);
            job.setErrorMessage(payload.getError());
            aiJobRepository.save(job);
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
            log.error("Webhook: processing failed for jobId={} type={}: {}",
                    job.getId(), job.getJobType(), e.getMessage(), e);
            job.setStatus(AiJobEntity.Status.FAILED);
            job.setErrorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        aiJobRepository.save(job);
        return ResponseEntity.ok().build();
    }

    private void handleIngestionResult(AiJobEntity job, AiWebhookPayload payload) {
        log.debug("Ingestion result: name='{}' category='{}' qty={} tags={}",
                payload.getName(), payload.getCategory(), payload.getQuantity(), payload.getTags());

        Item dto = new Item();
        dto.setName(payload.getName());
        dto.setDescription(payload.getDescription());
        dto.setQuantity(payload.getQuantity() != null ? payload.getQuantity() : 1);
        dto.setPurchasePrice(payload.getPurchasePrice());
        dto.setStorageId(job.getStorageId());

        if (payload.getCategory() != null && !payload.getCategory().isBlank()) {
            categoryRepository.findByName(payload.getCategory())
                    .ifPresentOrElse(
                            c -> dto.setCategoryId(c.getId()),
                            () -> log.warn("Ingestion: category '{}' not found, item created without category", payload.getCategory())
                    );
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
        log.info("Ingestion complete: itemId={} name='{}' storageId={} jobId={}",
                created.getId(), created.getName(), job.getStorageId(), job.getId());

        attachCapturedImage(job.getImagePath(), created.getId());

        job.setStatus(AiJobEntity.Status.DONE);
        job.setItemId(created.getId());
        if (payload.getTranscript() != null && !payload.getTranscript().isBlank()) {
            job.setWhisperTranscript(payload.getTranscript());
        }
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
        log.info("Analysis proposal ready: jobId={} type={} itemId={}", job.getId(), job.getJobType(), job.getItemId());
    }

    private void attachCapturedImage(String imagePath, Integer itemId) {
        if (imagePath == null || itemId == null) return;
        try {
            Path path = Path.of(imagePath);
            if (!Files.exists(path)) {
                log.warn("Captured image not found at path '{}' for itemId={}", imagePath, itemId);
                return;
            }

            byte[] raw = Files.readAllBytes(path);
            String mimeType = detectMimeType(imagePath);
            byte[] data = imageCompressionService.compress(raw, mimeType);

            var item = itemRepository.findById(itemId).orElse(null);
            if (item == null) {
                log.warn("Cannot attach image: item {} not found", itemId);
                return;
            }

            ImageEntity image = new ImageEntity();
            image.setData(data);
            image.setType(mimeType);
            image.setDateAdded(LocalDateTime.now());
            image.setDateModified(LocalDateTime.now());
            image.setItem(item);
            imageRepository.save(image);
            log.debug("Captured image attached to itemId={} ({}B → {}B compressed)", itemId, raw.length, data.length);
        } catch (Exception e) {
            log.warn("Failed to attach captured image for itemId={}: {}", itemId, e.getMessage());
        }
    }

    private String detectMimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private boolean isRetryableError(String error) {
        return error != null && error.contains("503");
    }
}

package de.iske.kistogramm.controller;

import de.iske.kistogramm.dto.AiWebhookPayload;
import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.model.AiJobEntity;
import de.iske.kistogramm.model.TagEntity;
import de.iske.kistogramm.repository.AiJobRepository;
import de.iske.kistogramm.repository.CategoryRepository;
import de.iske.kistogramm.repository.TagRepository;
import de.iske.kistogramm.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/ai/webhook")
public class AiWebhookController {

    @Value("${ai.webhook-secret}")
    private String webhookSecret;

    private final AiJobRepository aiJobRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ItemService itemService;

    public AiWebhookController(AiJobRepository aiJobRepository,
                                CategoryRepository categoryRepository,
                                TagRepository tagRepository,
                                ItemService itemService) {
        this.aiJobRepository = aiJobRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.itemService = itemService;
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

        job.setStatus(AiJobEntity.Status.PROCESSING);
        aiJobRepository.save(job);

        try {
            Item dto = new Item();
            dto.setName(payload.getName());
            dto.setDescription(payload.getDescription());
            dto.setQuantity(payload.getQuantity() != null ? payload.getQuantity() : 1);
            dto.setPurchasePrice(payload.getPurchasePrice());

            // Resolve category by name
            if (payload.getCategory() != null && !payload.getCategory().isBlank()) {
                categoryRepository.findByName(payload.getCategory())
                        .ifPresent(c -> dto.setCategoryId(c.getId()));
            }

            // Resolve or create tags by name
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

            job.setStatus(AiJobEntity.Status.DONE);
            job.setItemId(created.getId());
        } catch (Exception e) {
            job.setStatus(AiJobEntity.Status.FAILED);
            job.setErrorMessage(e.getMessage());
        }

        aiJobRepository.save(job);
        return ResponseEntity.ok().build();
    }
}

package de.iske.kistogramm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.AiJobResponse;
import de.iske.kistogramm.model.AiJobEntity;
import de.iske.kistogramm.model.ItemEntity;
import de.iske.kistogramm.model.TagEntity;
import de.iske.kistogramm.repository.AiJobRepository;
import de.iske.kistogramm.repository.ItemRepository;
import de.iske.kistogramm.repository.TagRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class AiJobService {

    private final AiJobRepository aiJobRepository;
    private final ItemRepository itemRepository;
    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper;

    public AiJobService(AiJobRepository aiJobRepository,
                        ItemRepository itemRepository,
                        TagRepository tagRepository,
                        ObjectMapper objectMapper) {
        this.aiJobRepository = aiJobRepository;
        this.itemRepository = itemRepository;
        this.tagRepository = tagRepository;
        this.objectMapper = objectMapper;
    }

    public AiJobResponse getJob(UUID jobId) {
        return AiJobResponse.from(
                aiJobRepository.findById(jobId)
                        .orElseThrow(() -> new NoSuchElementException("Job not found: " + jobId))
        );
    }

    public List<AiJobResponse> listJobs(@Nullable Integer itemId,
                                         @Nullable String jobTypeStr,
                                         @Nullable String statusStr) {
        AiJobEntity.JobType jobType = parseJobType(jobTypeStr);
        AiJobEntity.Status status = parseStatus(statusStr);

        List<AiJobEntity> jobs;
        if (itemId != null && jobType != null && status != null) {
            jobs = aiJobRepository.findByItemIdAndJobTypeAndStatus(itemId, jobType, status);
        } else if (itemId != null && jobType != null) {
            jobs = aiJobRepository.findByItemIdAndJobType(itemId, jobType);
        } else if (itemId != null && status != null) {
            jobs = aiJobRepository.findByItemIdAndStatus(itemId, status);
        } else if (jobType != null && status != null) {
            jobs = aiJobRepository.findByJobTypeAndStatus(jobType, status);
        } else if (itemId != null) {
            jobs = aiJobRepository.findByItemId(itemId);
        } else if (jobType != null) {
            jobs = aiJobRepository.findByJobType(jobType);
        } else if (status != null) {
            jobs = aiJobRepository.findByStatus(status);
        } else {
            jobs = aiJobRepository.findAll();
        }

        return jobs.stream().map(AiJobResponse::from).toList();
    }

    @Transactional
    public void cancelOrDeleteJob(UUID jobId) {
        AiJobEntity job = aiJobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException("Job not found: " + jobId));

        switch (job.getStatus()) {
            case PENDING -> {
                job.setStatus(AiJobEntity.Status.CANCELLED);
                aiJobRepository.save(job);
            }
            case PROCESSING -> throw new IllegalArgumentException(
                    "Job is currently being processed and cannot be cancelled. Wait for completion or failure.");
            case DONE, FAILED, CANCELLED -> aiJobRepository.delete(job);
        }
    }

    @Transactional
    public AiJobResponse acceptProposal(UUID jobId, @Nullable Map<String, Object> proposalOverride) {
        AiJobEntity job = aiJobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException("Job not found: " + jobId));

        if (job.getProposalStatus() != AiJobEntity.ProposalStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException(
                    "Job proposal is not pending review. Current status: " + job.getProposalStatus());
        }
        if (job.getJobType() == AiJobEntity.JobType.INGESTION) {
            throw new IllegalArgumentException("INGESTION jobs do not have proposals to accept.");
        }

        try {
            String dataJson = proposalOverride != null
                    ? objectMapper.writeValueAsString(proposalOverride)
                    : job.getProposalData();
            JsonNode data = objectMapper.readTree(dataJson);
            applyProposal(job, data);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to apply proposal: " + e.getMessage(), e);
        }

        job.setProposalStatus(AiJobEntity.ProposalStatus.ACCEPTED);
        aiJobRepository.save(job);
        return AiJobResponse.from(job);
    }

    @Transactional
    public AiJobResponse rejectProposal(UUID jobId) {
        AiJobEntity job = aiJobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException("Job not found: " + jobId));

        if (job.getProposalStatus() != AiJobEntity.ProposalStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException(
                    "Job proposal is not pending review. Current status: " + job.getProposalStatus());
        }

        job.setProposalStatus(AiJobEntity.ProposalStatus.REJECTED);
        aiJobRepository.save(job);
        return AiJobResponse.from(job);
    }

    private void applyProposal(AiJobEntity job, JsonNode data) {
        Integer itemId = job.getItemId();
        if (itemId == null) {
            throw new IllegalStateException("Job has no associated item.");
        }
        ItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + itemId));

        switch (job.getJobType()) {
            case DIMENSION_ESTIMATION -> applyDimensions(item, data);
            case VALUE_ESTIMATION -> applyValue(item, data);
            case CONDITION_ASSESSMENT -> applyCondition(item, data);
            case TAG_SUGGESTIONS -> applyTags(item, data);
            default -> throw new IllegalStateException("Unexpected job type: " + job.getJobType());
        }

        itemRepository.save(item);
    }

    private void applyDimensions(ItemEntity item, JsonNode data) {
        if (data.hasNonNull("width") && data.hasNonNull("widthUnit")) {
            item.getCustomAttributes().put("Breite", data.get("width").asText() + " " + data.get("widthUnit").asText());
        }
        if (data.hasNonNull("height") && data.hasNonNull("heightUnit")) {
            item.getCustomAttributes().put("Höhe", data.get("height").asText() + " " + data.get("heightUnit").asText());
        }
        if (data.hasNonNull("depth") && data.hasNonNull("depthUnit")) {
            item.getCustomAttributes().put("Tiefe", data.get("depth").asText() + " " + data.get("depthUnit").asText());
        }
        if (data.hasNonNull("weight") && data.hasNonNull("weightUnit")) {
            item.getCustomAttributes().put("Gewicht", data.get("weight").asText() + " " + data.get("weightUnit").asText());
        }
    }

    private void applyValue(ItemEntity item, JsonNode data) {
        if (data.hasNonNull("minValue") && data.hasNonNull("maxValue")) {
            double min = data.get("minValue").asDouble();
            double max = data.get("maxValue").asDouble();
            item.setPurchasePrice((min + max) / 2.0);
            String currency = data.hasNonNull("currency") ? data.get("currency").asText() : "EUR";
            item.getCustomAttributes().put("Marktwert",
                    String.format("%.2f – %.2f %s", min, max, currency));
        }
    }

    private void applyCondition(ItemEntity item, JsonNode data) {
        if (data.hasNonNull("condition")) {
            String condition = data.get("condition").asText();
            String details = data.hasNonNull("conditionDetails") ? data.get("conditionDetails").asText() : "";
            item.getCustomAttributes().put("Zustand",
                    details.isBlank() ? condition : condition + " – " + details);
        }
    }

    private void applyTags(ItemEntity item, JsonNode data) {
        if (data.hasNonNull("tags") && data.get("tags").isArray()) {
            for (JsonNode tagNode : data.get("tags")) {
                String tagName = tagNode.asText().trim().toLowerCase();
                if (tagName.isBlank()) continue;
                TagEntity tag = tagRepository.findByName(tagName).orElseGet(() -> {
                    TagEntity t = new TagEntity();
                    t.setName(tagName);
                    t.setDateAdded(LocalDateTime.now());
                    t.setDateModified(LocalDateTime.now());
                    return tagRepository.save(t);
                });
                item.getTags().add(tag);
            }
        }
    }

    private @Nullable AiJobEntity.JobType parseJobType(@Nullable String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return AiJobEntity.JobType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown jobType: " + value);
        }
    }

    private @Nullable AiJobEntity.Status parseStatus(@Nullable String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return AiJobEntity.Status.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown status: " + value);
        }
    }
}

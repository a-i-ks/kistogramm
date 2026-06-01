package de.iske.kistogramm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.model.AiJobEntity;
import de.iske.kistogramm.model.ImageEntity;
import de.iske.kistogramm.repository.AiJobRepository;
import de.iske.kistogramm.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
public class AiQueueService {

    private static final String QUEUE_KEY = "ai_jobs_queue";

    private final AiJobRepository aiJobRepository;
    private final ItemRepository itemRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.upload-dir}")
    private String uploadDir;

    public AiQueueService(AiJobRepository aiJobRepository,
                          ItemRepository itemRepository,
                          StringRedisTemplate redisTemplate,
                          ObjectMapper objectMapper) {
        this.aiJobRepository = aiJobRepository;
        this.itemRepository = itemRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public AiJobEntity submitJob(MultipartFile imageFile,
                                 MultipartFile audioFile,
                                 Integer storageId,
                                 Integer roomId) throws IOException {
        UUID jobId = UUID.randomUUID();
        Path jobDir = Path.of(uploadDir, jobId.toString());
        Files.createDirectories(jobDir);

        String imageExt = getExtension(imageFile.getOriginalFilename(), "jpg");
        Path imagePath = jobDir.resolve("image." + imageExt);
        imageFile.transferTo(imagePath);

        Path audioPath = null;
        if (audioFile != null && !audioFile.isEmpty()) {
            String audioExt = getExtension(audioFile.getOriginalFilename(), "wav");
            audioPath = jobDir.resolve("audio." + audioExt);
            audioFile.transferTo(audioPath);
        }

        AiJobEntity job = new AiJobEntity();
        job.setId(jobId);
        job.setStatus(AiJobEntity.Status.PENDING);
        job.setJobType(AiJobEntity.JobType.INGESTION);
        job.setImagePath(imagePath.toAbsolutePath().toString());
        job.setAudioPath(audioPath != null ? audioPath.toAbsolutePath().toString() : null);
        job.setStorageId(storageId);
        job.setRoomId(roomId);
        aiJobRepository.save(job);

        pushToQueue(jobId, imagePath.toAbsolutePath().toString(),
                audioPath != null ? audioPath.toAbsolutePath().toString() : null,
                AiJobEntity.JobType.INGESTION, null);
        return job;
    }

    public AiJobEntity submitAnalysisJob(Integer itemId, AiJobEntity.JobType jobType) throws IOException {
        if (jobType == AiJobEntity.JobType.INGESTION) {
            throw new IllegalArgumentException("Use submitJob() for INGESTION jobs.");
        }

        var item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + itemId));

        Set<ImageEntity> images = item.getImages();
        if (images == null || images.isEmpty()) {
            throw new IllegalStateException("Item " + itemId + " has no images. Analysis requires at least one image.");
        }

        UUID jobId = UUID.randomUUID();
        Path jobDir = Path.of(uploadDir, jobId.toString());
        Files.createDirectories(jobDir);

        ImageEntity firstImage = images.iterator().next();
        String ext = "jpg";
        if (firstImage.getType() != null) {
            if (firstImage.getType().contains("png")) ext = "png";
            else if (firstImage.getType().contains("webp")) ext = "webp";
        }
        Path imagePath = jobDir.resolve("image." + ext);
        Files.write(imagePath, firstImage.getData());

        AiJobEntity job = new AiJobEntity();
        job.setId(jobId);
        job.setStatus(AiJobEntity.Status.PENDING);
        job.setJobType(jobType);
        job.setItemId(itemId);
        job.setImagePath(imagePath.toAbsolutePath().toString());
        job.setProposalStatus(AiJobEntity.ProposalStatus.NONE);
        aiJobRepository.save(job);

        pushToQueue(jobId, imagePath.toAbsolutePath().toString(), null, jobType, itemId);
        return job;
    }

    public void cancelJob(UUID jobId) {
        AiJobEntity job = aiJobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException("Job not found: " + jobId));
        if (job.getStatus() == AiJobEntity.Status.PENDING) {
            job.setStatus(AiJobEntity.Status.CANCELLED);
            aiJobRepository.save(job);
        }
    }

    private void pushToQueue(UUID jobId, String imagePath, String audioPath,
                              AiJobEntity.JobType jobType, Integer itemId) {
        try {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("jobId", jobId.toString());
            payload.put("imagePath", imagePath != null ? imagePath : "");
            payload.put("audioPath", audioPath != null ? audioPath : "");
            payload.put("jobType", jobType.name());
            if (itemId != null) payload.put("itemId", itemId.toString());
            redisTemplate.opsForList().leftPush(QUEUE_KEY, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize job payload", e);
        }
    }

    private String getExtension(String filename, String fallback) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        }
        return fallback;
    }
}

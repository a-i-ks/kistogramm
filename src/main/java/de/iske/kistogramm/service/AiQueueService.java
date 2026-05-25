package de.iske.kistogramm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.model.AiJobEntity;
import de.iske.kistogramm.repository.AiJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
public class AiQueueService {

    private static final String QUEUE_KEY = "ai_jobs_queue";

    private final AiJobRepository aiJobRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.upload-dir}")
    private String uploadDir;

    public AiQueueService(AiJobRepository aiJobRepository,
                          StringRedisTemplate redisTemplate,
                          ObjectMapper objectMapper) {
        this.aiJobRepository = aiJobRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public AiJobEntity submitJob(MultipartFile imageFile, MultipartFile audioFile) throws IOException {
        UUID jobId = UUID.randomUUID();
        Path jobDir = Path.of(uploadDir, jobId.toString());
        Files.createDirectories(jobDir);

        String imageExt = getExtension(imageFile.getOriginalFilename(), "jpg");
        String audioExt = getExtension(audioFile.getOriginalFilename(), "wav");
        Path imagePath = jobDir.resolve("image." + imageExt);
        Path audioPath = jobDir.resolve("audio." + audioExt);

        imageFile.transferTo(imagePath);
        audioFile.transferTo(audioPath);

        AiJobEntity job = new AiJobEntity();
        job.setId(jobId);
        job.setStatus(AiJobEntity.Status.PENDING);
        job.setImagePath(imagePath.toAbsolutePath().toString());
        job.setAudioPath(audioPath.toAbsolutePath().toString());
        aiJobRepository.save(job);

        pushToQueue(jobId, imagePath, audioPath);
        return job;
    }

    private void pushToQueue(UUID jobId, Path imagePath, Path audioPath) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "jobId", jobId.toString(),
                    "imagePath", imagePath.toAbsolutePath().toString(),
                    "audioPath", audioPath.toAbsolutePath().toString()
            ));
            redisTemplate.opsForList().leftPush(QUEUE_KEY, payload);
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

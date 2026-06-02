package de.iske.kistogramm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.AiJobResponse;
import de.iske.kistogramm.model.AiJobEntity;
import de.iske.kistogramm.service.AiQueueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/ai")
public class AiIngestionController {

    private final AiQueueService aiQueueService;
    private final ObjectMapper objectMapper;

    public AiIngestionController(AiQueueService aiQueueService, ObjectMapper objectMapper) {
        this.aiQueueService = aiQueueService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/ingest")
    public ResponseEntity<AiJobResponse> ingest(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "audio", required = false) MultipartFile audio,
            @RequestParam(value = "metadata", required = false) String metadataJson) throws IOException {

        Integer storageId = null;
        Integer roomId = null;
        String contextHint = null;

        if (metadataJson != null && !metadataJson.isBlank()) {
            try {
                JsonNode meta = objectMapper.readTree(metadataJson);
                if (meta.hasNonNull("storageId")) storageId = meta.get("storageId").asInt();
                if (meta.hasNonNull("roomId"))    roomId    = meta.get("roomId").asInt();
                String title = meta.hasNonNull("title") ? meta.get("title").asText().strip() : "";
                String desc  = meta.hasNonNull("description") ? meta.get("description").asText().strip() : "";
                if (!title.isBlank() || !desc.isBlank()) {
                    contextHint = (title + ((!title.isBlank() && !desc.isBlank()) ? " – " : "") + desc).strip();
                }
            } catch (Exception ignored) {
                // malformed metadata is not fatal
            }
        }

        AiJobEntity job = aiQueueService.submitJob(image, audio, storageId, roomId, contextHint);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(AiJobResponse.from(job));
    }
}

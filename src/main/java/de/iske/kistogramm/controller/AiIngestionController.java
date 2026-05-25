package de.iske.kistogramm.controller;

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

    public AiIngestionController(AiQueueService aiQueueService) {
        this.aiQueueService = aiQueueService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<AiJobResponse> ingest(
            @RequestParam("image") MultipartFile image,
            @RequestParam("audio") MultipartFile audio) throws IOException {

        AiJobEntity job = aiQueueService.submitJob(image, audio);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new AiJobResponse(job.getId(), job.getStatus().name()));
    }
}

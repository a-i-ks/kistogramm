package de.iske.kistogramm.controller;

import de.iske.kistogramm.dto.AiJobResponse;
import de.iske.kistogramm.service.AiJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/jobs")
public class AiJobController {

    private final AiJobService aiJobService;

    public AiJobController(AiJobService aiJobService) {
        this.aiJobService = aiJobService;
    }

    @GetMapping
    public ResponseEntity<List<AiJobResponse>> listJobs(
            @RequestParam(required = false) Integer itemId,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(aiJobService.listJobs(itemId, jobType, status));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<AiJobResponse> getJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(aiJobService.getJob(jobId));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Integer>> deleteJobsBulk(
            @RequestParam(required = false) String status) {
        int count = aiJobService.deleteJobsByStatus(status);
        return ResponseEntity.ok(Map.of("deleted", count));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> cancelOrDeleteJob(@PathVariable UUID jobId) {
        aiJobService.cancelOrDeleteJob(jobId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{jobId}/start")
    public ResponseEntity<Void> markStarted(@PathVariable UUID jobId,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret) {
        aiJobService.markStarted(jobId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{jobId}/pause")
    public ResponseEntity<AiJobResponse> pauseJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(aiJobService.pauseJob(jobId));
    }

    @PostMapping("/{jobId}/resume")
    public ResponseEntity<AiJobResponse> resumeJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(aiJobService.resumeJob(jobId));
    }

    @PostMapping("/{jobId}/accept")
    public ResponseEntity<AiJobResponse> acceptProposal(
            @PathVariable UUID jobId,
            @RequestBody(required = false) Map<String, Object> proposalOverride) {
        return ResponseEntity.ok(aiJobService.acceptProposal(jobId, proposalOverride));
    }

    @PostMapping("/{jobId}/reject")
    public ResponseEntity<AiJobResponse> rejectProposal(@PathVariable UUID jobId) {
        return ResponseEntity.ok(aiJobService.rejectProposal(jobId));
    }
}

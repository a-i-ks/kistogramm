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

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> cancelOrDeleteJob(@PathVariable UUID jobId) {
        aiJobService.cancelOrDeleteJob(jobId);
        return ResponseEntity.noContent().build();
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

package de.iske.kistogramm.dto;

import java.util.UUID;

public class AiJobResponse {

    private UUID jobId;
    private String status;

    public AiJobResponse(UUID jobId, String status) {
        this.jobId = jobId;
        this.status = status;
    }

    public UUID getJobId() { return jobId; }
    public String getStatus() { return status; }
}

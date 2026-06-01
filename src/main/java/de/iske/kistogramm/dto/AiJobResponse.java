package de.iske.kistogramm.dto;

import de.iske.kistogramm.model.AiJobEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public class AiJobResponse {

    private UUID jobId;
    private String status;
    private String jobType;
    private Integer itemId;
    private String errorMessage;
    private String proposalData;
    private String proposalStatus;
    private LocalDateTime dateCreated;
    private LocalDateTime dateModified;

    public AiJobResponse(UUID jobId, String status, String jobType, Integer itemId,
                         String errorMessage, String proposalData, String proposalStatus,
                         LocalDateTime dateCreated, LocalDateTime dateModified) {
        this.jobId = jobId;
        this.status = status;
        this.jobType = jobType;
        this.itemId = itemId;
        this.errorMessage = errorMessage;
        this.proposalData = proposalData;
        this.proposalStatus = proposalStatus;
        this.dateCreated = dateCreated;
        this.dateModified = dateModified;
    }

    public static AiJobResponse from(AiJobEntity job) {
        return new AiJobResponse(
                job.getId(),
                job.getStatus().name(),
                job.getJobType().name(),
                job.getItemId(),
                job.getErrorMessage(),
                job.getProposalData(),
                job.getProposalStatus().name(),
                job.getDateCreated(),
                job.getDateModified()
        );
    }

    public UUID getJobId() { return jobId; }
    public String getStatus() { return status; }
    public String getJobType() { return jobType; }
    public Integer getItemId() { return itemId; }
    public String getErrorMessage() { return errorMessage; }
    public String getProposalData() { return proposalData; }
    public String getProposalStatus() { return proposalStatus; }
    public LocalDateTime getDateCreated() { return dateCreated; }
    public LocalDateTime getDateModified() { return dateModified; }
}

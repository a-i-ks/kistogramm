package de.iske.kistogramm.dto;

import de.iske.kistogramm.model.AiJobEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public class AiJobResponse {

    private UUID jobId;
    private String status;
    private String jobType;
    private Integer itemId;
    private String imagePath;
    private String audioPath;
    private String errorMessage;
    private String proposalData;
    private String proposalStatus;
    private String whisperTranscript;
    private String contextHint;
    private LocalDateTime dateCreated;
    private LocalDateTime dateStarted;
    private LocalDateTime dateModified;

    public AiJobResponse(UUID jobId, String status, String jobType, Integer itemId,
                         String imagePath, String audioPath, String errorMessage,
                         String proposalData, String proposalStatus, String whisperTranscript,
                         String contextHint, LocalDateTime dateCreated,
                         LocalDateTime dateStarted, LocalDateTime dateModified) {
        this.jobId = jobId;
        this.status = status;
        this.jobType = jobType;
        this.itemId = itemId;
        this.imagePath = imagePath;
        this.audioPath = audioPath;
        this.errorMessage = errorMessage;
        this.proposalData = proposalData;
        this.proposalStatus = proposalStatus;
        this.whisperTranscript = whisperTranscript;
        this.contextHint = contextHint;
        this.dateCreated = dateCreated;
        this.dateStarted = dateStarted;
        this.dateModified = dateModified;
    }

    public static AiJobResponse from(AiJobEntity job) {
        return new AiJobResponse(
                job.getId(),
                job.getStatus().name(),
                job.getJobType().name(),
                job.getItemId(),
                job.getImagePath(),
                job.getAudioPath(),
                job.getErrorMessage(),
                job.getProposalData(),
                job.getProposalStatus().name(),
                job.getWhisperTranscript(),
                job.getContextHint(),
                job.getDateCreated(),
                job.getDateStarted(),
                job.getDateModified()
        );
    }

    public UUID getJobId() { return jobId; }
    public String getStatus() { return status; }
    public String getJobType() { return jobType; }
    public Integer getItemId() { return itemId; }
    public String getImagePath() { return imagePath; }
    public String getAudioPath() { return audioPath; }
    public String getErrorMessage() { return errorMessage; }
    public String getProposalData() { return proposalData; }
    public String getProposalStatus() { return proposalStatus; }
    public String getWhisperTranscript() { return whisperTranscript; }
    public String getContextHint() { return contextHint; }
    public LocalDateTime getDateCreated() { return dateCreated; }
    public LocalDateTime getDateStarted() { return dateStarted; }
    public LocalDateTime getDateModified() { return dateModified; }
}

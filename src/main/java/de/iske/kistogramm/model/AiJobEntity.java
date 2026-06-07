package de.iske.kistogramm.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_jobs")
public class AiJobEntity {

    public enum Status {
        PENDING, PAUSED, PROCESSING, DONE, FAILED, CANCELLED
    }

    public enum JobType {
        INGESTION, DIMENSION_ESTIMATION, VALUE_ESTIMATION, CONDITION_ASSESSMENT, TAG_SUGGESTIONS
    }

    public enum ProposalStatus {
        NONE, PENDING_REVIEW, ACCEPTED, REJECTED
    }

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private JobType jobType = JobType.INGESTION;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposal_status", nullable = false, length = 30)
    private ProposalStatus proposalStatus = ProposalStatus.NONE;

    @Column(name = "image_path", length = 1024)
    private String imagePath;

    @Column(name = "audio_path", length = 1024)
    private String audioPath;

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "storage_id")
    private Integer storageId;

    @Column(name = "room_id")
    private Integer roomId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "proposal_data", columnDefinition = "TEXT")
    private String proposalData;

    @Column(name = "whisper_transcript", columnDefinition = "TEXT")
    private String whisperTranscript;

    @Column(name = "context_hint", columnDefinition = "TEXT")
    private String contextHint;

    @Column(name = "capture_metadata", columnDefinition = "TEXT")
    private String captureMetadata;

    @Column(name = "date_started")
    private LocalDateTime dateStarted;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @Column(name = "date_modified")
    private LocalDateTime dateModified;

    @PrePersist
    public void onCreate() {
        if (id == null) id = UUID.randomUUID();
        dateCreated = LocalDateTime.now();
        dateModified = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        dateModified = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public JobType getJobType() { return jobType; }
    public void setJobType(JobType jobType) { this.jobType = jobType; }

    public ProposalStatus getProposalStatus() { return proposalStatus; }
    public void setProposalStatus(ProposalStatus proposalStatus) { this.proposalStatus = proposalStatus; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public Integer getStorageId() { return storageId; }
    public void setStorageId(Integer storageId) { this.storageId = storageId; }

    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getProposalData() { return proposalData; }
    public void setProposalData(String proposalData) { this.proposalData = proposalData; }

    public String getWhisperTranscript() { return whisperTranscript; }
    public void setWhisperTranscript(String whisperTranscript) { this.whisperTranscript = whisperTranscript; }

    public String getContextHint() { return contextHint; }
    public void setContextHint(String contextHint) { this.contextHint = contextHint; }

    public String getCaptureMetadata() { return captureMetadata; }
    public void setCaptureMetadata(String captureMetadata) { this.captureMetadata = captureMetadata; }

    public LocalDateTime getDateStarted() { return dateStarted; }
    public void setDateStarted(LocalDateTime dateStarted) { this.dateStarted = dateStarted; }

    public LocalDateTime getDateCreated() { return dateCreated; }
    public LocalDateTime getDateModified() { return dateModified; }
}

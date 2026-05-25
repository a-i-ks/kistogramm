package de.iske.kistogramm.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_jobs")
public class AiJobEntity {

    public enum Status {
        PENDING, PROCESSING, DONE, FAILED
    }

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "image_path", length = 1024)
    private String imagePath;

    @Column(name = "audio_path", length = 1024)
    private String audioPath;

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

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

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getDateCreated() { return dateCreated; }
    public LocalDateTime getDateModified() { return dateModified; }
}

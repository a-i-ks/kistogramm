package de.iske.kistogramm.repository;

import de.iske.kistogramm.model.AiJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AiJobRepository extends JpaRepository<AiJobEntity, UUID> {

    List<AiJobEntity> findByItemId(Integer itemId);

    List<AiJobEntity> findByJobType(AiJobEntity.JobType jobType);

    List<AiJobEntity> findByStatus(AiJobEntity.Status status);

    List<AiJobEntity> findByItemIdAndJobType(Integer itemId, AiJobEntity.JobType jobType);

    List<AiJobEntity> findByItemIdAndStatus(Integer itemId, AiJobEntity.Status status);

    List<AiJobEntity> findByJobTypeAndStatus(AiJobEntity.JobType jobType, AiJobEntity.Status status);

    List<AiJobEntity> findByItemIdAndJobTypeAndStatus(Integer itemId, AiJobEntity.JobType jobType, AiJobEntity.Status status);

    List<AiJobEntity> findByStatusAndNextRetryAtBefore(AiJobEntity.Status status, LocalDateTime time);
}

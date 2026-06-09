package de.iske.kistogramm.service;

import de.iske.kistogramm.model.AiJobEntity;
import de.iske.kistogramm.repository.AiJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AiRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(AiRetryScheduler.class);

    private final AiJobRepository aiJobRepository;
    private final AiQueueService aiQueueService;

    public AiRetryScheduler(AiJobRepository aiJobRepository, AiQueueService aiQueueService) {
        this.aiJobRepository = aiJobRepository;
        this.aiQueueService = aiQueueService;
    }

    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void requeueDueRetries() {
        List<AiJobEntity> jobs = aiJobRepository.findByStatusAndNextRetryAtBefore(
                AiJobEntity.Status.AWAITING_RETRY, LocalDateTime.now());
        for (AiJobEntity job : jobs) {
            job.setStatus(AiJobEntity.Status.PENDING);
            job.setNextRetryAt(null);
            aiJobRepository.save(job);
            aiQueueService.requeueJob(job);
            log.warn("Retry {}: job {} re-queued", job.getRetryCount(), job.getId());
        }
    }
}

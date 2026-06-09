package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.AppSettingsDto;
import de.iske.kistogramm.model.AppSettingsEntity;
import de.iske.kistogramm.repository.AppSettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class AppSettingsService {

    private static final int SETTINGS_ID = 1;

    private final AppSettingsRepository repository;

    public AppSettingsService(AppSettingsRepository repository) {
        this.repository = repository;
    }

    public AppSettingsDto getSettings() {
        AppSettingsEntity entity = repository.findById(SETTINGS_ID)
                .orElseGet(() -> repository.save(new AppSettingsEntity()));
        return toDto(entity);
    }

    public AppSettingsDto updateSettings(AppSettingsDto dto) {
        AppSettingsEntity entity = repository.findById(SETTINGS_ID)
                .orElseGet(AppSettingsEntity::new);
        entity.setId(SETTINGS_ID);
        entity.setImageCompressionEnabled(dto.isImageCompressionEnabled());
        entity.setImageMaxWidth(dto.getImageMaxWidth());
        entity.setImageMaxHeight(dto.getImageMaxHeight());
        entity.setImageQuality(dto.getImageQuality());
        entity.setVlmProvider(dto.getVlmProvider() != null ? dto.getVlmProvider() : "ollama");
        entity.setOpenaiApiKey(dto.getOpenaiApiKey());
        entity.setGeminiApiKey(dto.getGeminiApiKey());
        entity.setVlmModel(dto.getVlmModel());
        entity.setVlmDevice(dto.getVlmDevice());
        entity.setVlmNumCtx(dto.getVlmNumCtx());
        entity.setVlmNumThread(dto.getVlmNumThread());
        entity.setVlmImageCompressionEnabled(dto.isVlmImageCompressionEnabled());
        entity.setVlmImageMaxWidth(dto.getVlmImageMaxWidth());
        entity.setVlmImageMaxHeight(dto.getVlmImageMaxHeight());
        entity.setVlmImageQuality(dto.getVlmImageQuality());
        entity.setAiRetryEnabled(dto.isAiRetryEnabled());
        entity.setAiRetryMaxAttempts(dto.getAiRetryMaxAttempts());
        entity.setAiRetryDelaySeconds(dto.getAiRetryDelaySeconds());
        return toDto(repository.save(entity));
    }

    public AppSettingsEntity getSettingsEntity() {
        return repository.findById(SETTINGS_ID)
                .orElseGet(() -> repository.save(new AppSettingsEntity()));
    }

    private AppSettingsDto toDto(AppSettingsEntity entity) {
        AppSettingsDto dto = new AppSettingsDto();
        dto.setImageCompressionEnabled(entity.isImageCompressionEnabled());
        dto.setImageMaxWidth(entity.getImageMaxWidth());
        dto.setImageMaxHeight(entity.getImageMaxHeight());
        dto.setImageQuality(entity.getImageQuality());
        dto.setVlmProvider(entity.getVlmProvider() != null ? entity.getVlmProvider() : "ollama");
        dto.setOpenaiApiKey(entity.getOpenaiApiKey());
        dto.setGeminiApiKey(entity.getGeminiApiKey());
        dto.setVlmModel(entity.getVlmModel());
        dto.setVlmDevice(entity.getVlmDevice());
        dto.setVlmNumCtx(entity.getVlmNumCtx());
        dto.setVlmNumThread(entity.getVlmNumThread());
        dto.setVlmImageCompressionEnabled(entity.isVlmImageCompressionEnabled());
        dto.setVlmImageMaxWidth(entity.getVlmImageMaxWidth());
        dto.setVlmImageMaxHeight(entity.getVlmImageMaxHeight());
        dto.setVlmImageQuality(entity.getVlmImageQuality());
        dto.setAiRetryEnabled(entity.isAiRetryEnabled());
        dto.setAiRetryMaxAttempts(entity.getAiRetryMaxAttempts());
        dto.setAiRetryDelaySeconds(entity.getAiRetryDelaySeconds());
        return dto;
    }
}

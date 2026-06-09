package de.iske.kistogramm.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_settings")
public class AppSettingsEntity {

    @Id
    private Integer id = 1;

    private boolean imageCompressionEnabled = true;
    private int imageMaxWidth = 1920;
    private int imageMaxHeight = 1080;
    private int imageQuality = 85;

    private String vlmProvider = "ollama";
    private String openaiApiKey;
    private String geminiApiKey;

    private String vlmModel = "qwen2.5vl:7b";
    private String vlmDevice = "auto";
    private int vlmNumCtx = 4096;
    private int vlmNumThread = 4;

    private boolean vlmImageCompressionEnabled = true;
    private int vlmImageMaxWidth = 672;
    private int vlmImageMaxHeight = 448;
    private int vlmImageQuality = 85;

    private boolean aiRetryEnabled = true;
    private int aiRetryMaxAttempts = 3;
    private int aiRetryDelaySeconds = 30;

    private String logLevel = "INFO";

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean isImageCompressionEnabled() {
        return imageCompressionEnabled;
    }

    public void setImageCompressionEnabled(boolean imageCompressionEnabled) {
        this.imageCompressionEnabled = imageCompressionEnabled;
    }

    public int getImageMaxWidth() {
        return imageMaxWidth;
    }

    public void setImageMaxWidth(int imageMaxWidth) {
        this.imageMaxWidth = imageMaxWidth;
    }

    public int getImageMaxHeight() {
        return imageMaxHeight;
    }

    public void setImageMaxHeight(int imageMaxHeight) {
        this.imageMaxHeight = imageMaxHeight;
    }

    public int getImageQuality() {
        return imageQuality;
    }

    public void setImageQuality(int imageQuality) {
        this.imageQuality = imageQuality;
    }

    public String getVlmProvider() {
        return vlmProvider;
    }

    public void setVlmProvider(String vlmProvider) {
        this.vlmProvider = vlmProvider;
    }

    public String getOpenaiApiKey() {
        return openaiApiKey;
    }

    public void setOpenaiApiKey(String openaiApiKey) {
        this.openaiApiKey = openaiApiKey;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public String getVlmModel() {
        return vlmModel;
    }

    public void setVlmModel(String vlmModel) {
        this.vlmModel = vlmModel;
    }

    public String getVlmDevice() {
        return vlmDevice;
    }

    public void setVlmDevice(String vlmDevice) {
        this.vlmDevice = vlmDevice;
    }

    public int getVlmNumCtx() {
        return vlmNumCtx;
    }

    public void setVlmNumCtx(int vlmNumCtx) {
        this.vlmNumCtx = vlmNumCtx;
    }

    public int getVlmNumThread() {
        return vlmNumThread;
    }

    public void setVlmNumThread(int vlmNumThread) {
        this.vlmNumThread = vlmNumThread;
    }

    public boolean isVlmImageCompressionEnabled() {
        return vlmImageCompressionEnabled;
    }

    public void setVlmImageCompressionEnabled(boolean vlmImageCompressionEnabled) {
        this.vlmImageCompressionEnabled = vlmImageCompressionEnabled;
    }

    public int getVlmImageMaxWidth() {
        return vlmImageMaxWidth;
    }

    public void setVlmImageMaxWidth(int vlmImageMaxWidth) {
        this.vlmImageMaxWidth = vlmImageMaxWidth;
    }

    public int getVlmImageMaxHeight() {
        return vlmImageMaxHeight;
    }

    public void setVlmImageMaxHeight(int vlmImageMaxHeight) {
        this.vlmImageMaxHeight = vlmImageMaxHeight;
    }

    public int getVlmImageQuality() {
        return vlmImageQuality;
    }

    public void setVlmImageQuality(int vlmImageQuality) {
        this.vlmImageQuality = vlmImageQuality;
    }

    public boolean isAiRetryEnabled() {
        return aiRetryEnabled;
    }

    public void setAiRetryEnabled(boolean aiRetryEnabled) {
        this.aiRetryEnabled = aiRetryEnabled;
    }

    public int getAiRetryMaxAttempts() {
        return aiRetryMaxAttempts;
    }

    public void setAiRetryMaxAttempts(int aiRetryMaxAttempts) {
        this.aiRetryMaxAttempts = aiRetryMaxAttempts;
    }

    public int getAiRetryDelaySeconds() {
        return aiRetryDelaySeconds;
    }

    public void setAiRetryDelaySeconds(int aiRetryDelaySeconds) {
        this.aiRetryDelaySeconds = aiRetryDelaySeconds;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }
}

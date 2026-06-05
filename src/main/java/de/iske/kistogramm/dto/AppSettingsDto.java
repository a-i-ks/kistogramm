package de.iske.kistogramm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AppSettingsDto {

    private boolean imageCompressionEnabled;

    @Min(1)
    @Max(10000)
    private int imageMaxWidth = 1920;

    @Min(1)
    @Max(10000)
    private int imageMaxHeight = 1080;

    @Min(1)
    @Max(100)
    private int imageQuality = 85;

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

    @Pattern(regexp = "ollama|openai|gemini")
    private String vlmProvider = "ollama";

    @Size(max = 255)
    private String openaiApiKey;

    @Size(max = 255)
    private String geminiApiKey;

    @NotBlank
    @Size(max = 100)
    private String vlmModel = "qwen2.5vl:7b";

    @Pattern(regexp = "auto|gpu|cpu")
    private String vlmDevice = "auto";

    @Min(512)
    @Max(131072)
    private int vlmNumCtx = 4096;

    @Min(1)
    @Max(64)
    private int vlmNumThread = 4;

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

    private boolean vlmImageCompressionEnabled = true;

    @Min(64)
    @Max(4096)
    private int vlmImageMaxWidth = 672;

    @Min(64)
    @Max(4096)
    private int vlmImageMaxHeight = 448;

    @Min(1)
    @Max(100)
    private int vlmImageQuality = 85;

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
}

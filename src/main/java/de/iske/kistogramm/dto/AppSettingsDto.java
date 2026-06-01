package de.iske.kistogramm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

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
}

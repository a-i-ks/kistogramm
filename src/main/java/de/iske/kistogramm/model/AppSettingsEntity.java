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

    private String vlmModel = "qwen2.5vl:7b";
    private String vlmDevice = "auto";
    private int vlmNumCtx = 4096;
    private int vlmNumThread = 4;

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
}

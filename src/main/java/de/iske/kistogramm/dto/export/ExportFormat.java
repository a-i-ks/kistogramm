package de.iske.kistogramm.dto.export;

public enum ExportFormat {
    CSV("text/csv"),
    JSON("application/json"),
    XML("application/xml");

    private final String mimeType;

    ExportFormat(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }
}

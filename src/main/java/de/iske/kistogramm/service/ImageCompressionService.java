package de.iske.kistogramm.service;

import de.iske.kistogramm.model.AppSettingsEntity;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ImageCompressionService {

    private final AppSettingsService settingsService;

    public ImageCompressionService(AppSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public byte[] compress(byte[] imageData, String contentType) {
        if (imageData == null || imageData.length == 0) {
            return imageData;
        }

        AppSettingsEntity settings = settingsService.getSettingsEntity();

        if (!settings.isImageCompressionEnabled()) {
            return imageData;
        }

        if (contentType == null || !contentType.startsWith("image/")) {
            return imageData;
        }

        // GIF-Animationen nicht anfassen
        if ("image/gif".equals(contentType)) {
            return imageData;
        }

        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageData));
            if (original == null) {
                return imageData;
            }

            int maxWidth = settings.getImageMaxWidth();
            int maxHeight = settings.getImageMaxHeight();
            double quality = settings.getImageQuality() / 100.0;

            boolean needsResize = original.getWidth() > maxWidth || original.getHeight() > maxHeight;
            boolean isJpeg = "image/jpeg".equals(contentType) || "image/jpg".equals(contentType);

            // PNG ohne Resize-Bedarf überspringen — PNG-Komprimierung ist verlustfrei
            // und wird von thumbnailator nicht durch quality gesteuert
            if (!needsResize && !isJpeg) {
                return imageData;
            }

            String formatName = isJpeg ? "jpg" : formatFromContentType(contentType);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (needsResize) {
                Thumbnails.of(new ByteArrayInputStream(imageData))
                        .size(maxWidth, maxHeight)
                        .keepAspectRatio(true)
                        .outputFormat(formatName)
                        .outputQuality(quality)
                        .toOutputStream(out);
            } else {
                Thumbnails.of(new ByteArrayInputStream(imageData))
                        .scale(1.0)
                        .outputFormat(formatName)
                        .outputQuality(quality)
                        .toOutputStream(out);
            }

            byte[] compressed = out.toByteArray();
            // Nur verwenden wenn kleiner als Original
            return compressed.length < imageData.length ? compressed : imageData;

        } catch (IOException e) {
            return imageData;
        }
    }

    private String formatFromContentType(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}

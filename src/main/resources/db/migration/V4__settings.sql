CREATE TABLE app_settings (
    id INT NOT NULL,
    image_compression_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    image_max_width INT NOT NULL DEFAULT 1920,
    image_max_height INT NOT NULL DEFAULT 1080,
    image_quality INT NOT NULL DEFAULT 85,
    CONSTRAINT pk_app_settings PRIMARY KEY (id)
);

INSERT INTO app_settings (id, image_compression_enabled, image_max_width, image_max_height, image_quality)
VALUES (1, FALSE, 1920, 1080, 85);

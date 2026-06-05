ALTER TABLE app_settings ADD COLUMN vlm_image_compression_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_settings ADD COLUMN vlm_image_max_width INTEGER NOT NULL DEFAULT 672;
ALTER TABLE app_settings ADD COLUMN vlm_image_max_height INTEGER NOT NULL DEFAULT 448;
ALTER TABLE app_settings ADD COLUMN vlm_image_quality INTEGER NOT NULL DEFAULT 85;

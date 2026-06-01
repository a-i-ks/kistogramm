ALTER TABLE ai_jobs ADD COLUMN job_type VARCHAR(50) NOT NULL DEFAULT 'INGESTION';
ALTER TABLE ai_jobs ADD COLUMN proposal_data TEXT;
ALTER TABLE ai_jobs ADD COLUMN proposal_status VARCHAR(30) NOT NULL DEFAULT 'NONE';

UPDATE app_settings SET image_compression_enabled = TRUE WHERE id = 1;

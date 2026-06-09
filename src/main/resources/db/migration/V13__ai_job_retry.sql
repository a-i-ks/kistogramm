ALTER TABLE ai_jobs ADD COLUMN retry_count INT NOT NULL DEFAULT 0;
ALTER TABLE ai_jobs ADD COLUMN next_retry_at TIMESTAMP;

ALTER TABLE app_settings ADD COLUMN ai_retry_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_settings ADD COLUMN ai_retry_max_attempts INT NOT NULL DEFAULT 3;
ALTER TABLE app_settings ADD COLUMN ai_retry_delay_seconds INT NOT NULL DEFAULT 30;

ALTER TABLE app_settings ADD COLUMN vlm_provider VARCHAR(20) NOT NULL DEFAULT 'ollama';
ALTER TABLE app_settings ADD COLUMN openai_api_key VARCHAR(255);
ALTER TABLE app_settings ADD COLUMN gemini_api_key VARCHAR(255);

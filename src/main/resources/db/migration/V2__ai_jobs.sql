CREATE TABLE IF NOT EXISTS ai_jobs (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    image_path VARCHAR(1024),
    audio_path VARCHAR(1024),
    item_id INT,
    error_message TEXT,
    date_created TIMESTAMP,
    date_modified TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES items(id)
);

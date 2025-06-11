ALTER TABLE images ADD COLUMN receipt_item_id INT;
ALTER TABLE images ADD CONSTRAINT fk_images_receipt_item_id FOREIGN KEY (receipt_item_id) REFERENCES items(id);

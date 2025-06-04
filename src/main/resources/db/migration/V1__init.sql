-- ==========================================
-- Flyway Migration: V1__init.sql (H2 Compatible)
-- Home Inventory Schema Initialization (REVISED)
-- ==========================================

-- Categories
CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    date_added TIMESTAMP,
    date_modified TIMESTAMP
);

-- Rooms
CREATE TABLE IF NOT EXISTS rooms (
    id SERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    image_id INT,
    date_added TIMESTAMP,
    date_modified TIMESTAMP
);

-- Tags
CREATE TABLE IF NOT EXISTS tags (
    id SERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    date_added TIMESTAMP,
    date_modified TIMESTAMP
);

-- Storages
CREATE TABLE IF NOT EXISTS storages (
    id SERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    room_id INT,
    parent_storage_id INT,
    date_added TIMESTAMP,
    date_modified TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id),
    FOREIGN KEY (parent_storage_id) REFERENCES storages(id)
);

-- Items
CREATE TABLE IF NOT EXISTS items (
    id SERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    purchase_date DATE,
    purchase_price DOUBLE PRECISION,
    quantity INT,
    category_id INT,
    storage_id INT,
    date_added TIMESTAMP,
    date_modified TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (storage_id) REFERENCES storages(id)
);

-- Images
CREATE TABLE IF NOT EXISTS images (
    id SERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    description TEXT,
    data BYTEA NOT NULL,
    date_added TIMESTAMP,
    date_modified TIMESTAMP,
    item_id INT,
    storage_id INT,
    room_id INT,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (storage_id) REFERENCES storages(id),
    FOREIGN KEY (room_id) REFERENCES rooms(id)
);

-- Item - Related Items
CREATE TABLE IF NOT EXISTS item_related (
    item_id INT NOT NULL,
    related_item_id INT NOT NULL,
    PRIMARY KEY (item_id, related_item_id),
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (related_item_id) REFERENCES items(id)
);

-- Item - Tag
CREATE TABLE IF NOT EXISTS item_tags (
    item_id INT NOT NULL,
    tag_id INT NOT NULL,
    PRIMARY KEY (item_id, tag_id),
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (tag_id) REFERENCES tags(id)
);

-- Storage - Tag
CREATE TABLE IF NOT EXISTS storage_tags (
    storage_id INT NOT NULL,
    tag_id INT NOT NULL,
    PRIMARY KEY (storage_id, tag_id),
    FOREIGN KEY (storage_id) REFERENCES storages(id),
    FOREIGN KEY (tag_id) REFERENCES tags(id)
);

-- Item - Dynamic Attributes
CREATE TABLE IF NOT EXISTS item_attributes (
    item_id INT NOT NULL,
    attribute_key VARCHAR(255) NOT NULL,
    attribute_value TEXT,
    PRIMARY KEY (item_id, attribute_key),
    FOREIGN KEY (item_id) REFERENCES items(id)
);

-- Category Attribute Templates
CREATE TABLE IF NOT EXISTS category_attribute_templates (
    id SERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    category_id INT NOT NULL,
    attribute_name VARCHAR(255) NOT NULL,
    date_added TIMESTAMP,
    date_modified TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);
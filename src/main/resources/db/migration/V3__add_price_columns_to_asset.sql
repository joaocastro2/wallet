ALTER TABLE asset
    ADD COLUMN current_price NUMERIC(15, 4),
    ADD COLUMN last_price_update TIMESTAMP;
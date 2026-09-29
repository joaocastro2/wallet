CREATE TABLE asset_price_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL,
    price DECIMAL(19, 4) NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_asset_price_history_asset FOREIGN KEY (asset_id) REFERENCES asset (id)
);

CREATE INDEX idx_price_history_asset_date ON asset_price_history(asset_id, recorded_at DESC);
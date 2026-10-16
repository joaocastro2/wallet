ALTER TABLE wallet_position ADD COLUMN user_id UUID NOT NULL;
CREATE INDEX idx_wallet_positions_user_asset ON wallet_position(user_id, asset_id);
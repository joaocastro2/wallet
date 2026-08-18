-- Enum do Tipo de Operação (BUY ou SELL)
CREATE TYPE transaction_type AS ENUM ('BUY', 'SELL');

-- Tabela de Transações (Extrato/Histórico)
CREATE TABLE transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL,
    type VARCHAR(10) NOT NULL, -- BUY ou SELL
    quantity DECIMAL(19, 4) NOT NULL,
    unit_price DECIMAL(19, 4) NOT NULL,
    total_value DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_asset FOREIGN KEY (asset_id) REFERENCES asset (id)
);

-- Tabela de Posição da Carteira
CREATE TABLE wallet_position (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL UNIQUE,
    quantity DECIMAL(19, 4) NOT NULL DEFAULT 0,
    average_price DECIMAL(19, 4) NOT NULL DEFAULT 0, -- Preço Médio de Compra
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_position_asset FOREIGN KEY (asset_id) REFERENCES asset (id)
);

CREATE INDEX idx_transaction_asset_created ON transaction(asset_id, created_at DESC);
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE asset (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        symbol VARCHAR(20) NOT NULL UNIQUE,
        name VARCHAR(100) NOT NULL,
        type VARCHAR(30) NOT NULL,
        active BOOLEAN NOT NULL DEFAULT TRUE
);
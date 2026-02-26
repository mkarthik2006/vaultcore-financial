CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number VARCHAR(32) NOT NULL UNIQUE,
    currency VARCHAR(3) NOT NULL
);

ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_currency_len
    CHECK (char_length(currency) = 3);

CREATE INDEX idx_accounts_account_number ON accounts(account_number);

CREATE TABLE transaction_references (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
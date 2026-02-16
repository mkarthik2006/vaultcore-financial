CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL,
    account_id UUID NOT NULL,
    entry_type VARCHAR(6) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

ALTER TABLE ledger_entries
    ADD CONSTRAINT chk_ledger_entry_type
    CHECK (entry_type IN ('DEBIT', 'CREDIT'));

ALTER TABLE ledger_entries
    ADD CONSTRAINT chk_ledger_amount_positive
    CHECK (amount > 0);

CREATE INDEX idx_ledger_entries_account_id ON ledger_entries(account_id);
CREATE INDEX idx_ledger_entries_transaction_id ON ledger_entries(transaction_id);

CREATE OR REPLACE FUNCTION ledger_entries_immutable()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Ledger entries are immutable. UPDATE/DELETE not allowed.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_entries_no_update
BEFORE UPDATE OR DELETE ON ledger_entries
FOR EACH ROW EXECUTE FUNCTION ledger_entries_immutable();

CREATE OR REPLACE FUNCTION validate_double_entry()
RETURNS trigger AS $$
DECLARE
    debit_sum NUMERIC(19,4);
    credit_sum NUMERIC(19,4);
    entry_count INT;
BEGIN
    SELECT
        COALESCE(SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END), 0),
        COUNT(*)
    INTO debit_sum, credit_sum, entry_count
    FROM ledger_entries
    WHERE transaction_id = NEW.transaction_id;

    IF entry_count < 2 THEN
        RAISE EXCEPTION 'Double-entry requires at least 2 entries per transaction_id';
    END IF;

    IF debit_sum <> credit_sum THEN
        RAISE EXCEPTION 'Double-entry imbalance for transaction_id: %, debit=%, credit=%',
            NEW.transaction_id, debit_sum, credit_sum;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_validate_double_entry
AFTER INSERT ON ledger_entries
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION validate_double_entry();
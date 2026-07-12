-- Idempotency support for money-movement endpoints.
-- A client-supplied Idempotency-Key is reserved BEFORE the transfer executes; the UNIQUE index is
-- the concurrency backstop that guarantees at most one execution per key even under simultaneous
-- duplicate submissions. The stored result is replayed on any subsequent request with the same key.
CREATE TABLE idempotency_keys (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key          VARCHAR(200) NOT NULL,
    request_fingerprint      VARCHAR(128) NOT NULL,
    status                   VARCHAR(20)  NOT NULL,
    transaction_reference_id UUID,
    ledger_transaction_id    UUID,
    created_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at             TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE idempotency_keys
    ADD CONSTRAINT chk_idempotency_status
    CHECK (status IN ('IN_PROGRESS', 'COMPLETED'));

CREATE UNIQUE INDEX idx_idempotency_key ON idempotency_keys(idempotency_key);

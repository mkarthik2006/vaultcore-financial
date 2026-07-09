-- Fraud 2FA challenges. A transfer at/above the configured threshold issues a PENDING challenge
-- (mock SMS/email code, one-way hashed). The customer verifies the code (VERIFIED), then resubmits
-- the transfer referencing the challenge, which is CONSUMED on success. Challenges expire.
CREATE TABLE fraud_challenges (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_account  VARCHAR(32)  NOT NULL,
    amount        NUMERIC(19,4) NOT NULL,
    currency      VARCHAR(3)   NOT NULL,
    code_hash     VARCHAR(128) NOT NULL,
    channel       VARCHAR(40)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    verified_at   TIMESTAMP WITHOUT TIME ZONE,
    consumed_at   TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE fraud_challenges
    ADD CONSTRAINT chk_fraud_challenge_status
    CHECK (status IN ('PENDING', 'VERIFIED', 'CONSUMED', 'EXPIRED'));

CREATE INDEX idx_fraud_challenges_from_account ON fraud_challenges(from_account);

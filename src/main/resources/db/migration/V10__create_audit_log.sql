-- Durable, queryable audit trail for security-relevant events (transfers, fraud challenges).
-- Complements the SLF4J audit aspect with a record that survives container/log rotation.
CREATE TABLE audit_log (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action         VARCHAR(60)   NOT NULL,
    principal      VARCHAR(200),
    detail         VARCHAR(1000),
    correlation_id VARCHAR(64),
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_created ON audit_log (created_at);
CREATE INDEX idx_audit_log_principal ON audit_log (principal);

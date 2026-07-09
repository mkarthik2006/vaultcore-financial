package com.vaultcore.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Records security-relevant events into the durable {@code audit_log} table. Writes are best-effort:
 * an audit failure must never break the business operation that triggered it, so callers are also
 * defensive and this method swallows persistence errors after logging them.
 */
@Service
public class AuditEventService {

    private static final Logger log = LoggerFactory.getLogger(AuditEventService.class);

    private final AuditEventRepository repository;

    public AuditEventService(AuditEventRepository repository) {
        this.repository = repository;
    }

    public void record(String action, String principal, String detail) {
        try {
            repository.save(new AuditEntry(action, principal, detail, MDC.get("correlationId")));
        } catch (RuntimeException ex) {
            log.warn("Failed to persist audit event action={} principal={}", action, principal, ex);
        }
    }
}

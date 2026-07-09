package com.vaultcore.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** A single durable audit record for a security-relevant event. */
@Entity
@Table(name = "audit_log")
public class AuditEntry {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "action", nullable = false, updatable = false, length = 60)
    private String action;

    @Column(name = "principal", updatable = false, length = 200)
    private String principal;

    @Column(name = "detail", updatable = false, length = 1000)
    private String detail;

    @Column(name = "correlation_id", updatable = false, length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEntry() {
        // JPA
    }

    public AuditEntry(String action, String principal, String detail, String correlationId) {
        this.action = action;
        this.principal = principal;
        this.detail = detail == null ? null : detail.substring(0, Math.min(detail.length(), 1000));
        this.correlationId = correlationId;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getAction() { return action; }
    public String getPrincipal() { return principal; }
    public String getDetail() { return detail; }
    public String getCorrelationId() { return correlationId; }
    public Instant getCreatedAt() { return createdAt; }
}

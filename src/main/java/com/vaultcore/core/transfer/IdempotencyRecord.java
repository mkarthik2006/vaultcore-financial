package com.vaultcore.core.transfer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent record of a client Idempotency-Key. Reserved as {@code IN_PROGRESS} before a transfer
 * runs and updated to {@code COMPLETED} with the resulting identifiers, which are replayed on any
 * repeat request carrying the same key.
 */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {

    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, updatable = false, length = 128)
    private String requestFingerprint;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "transaction_reference_id")
    private UUID transactionReferenceId;

    @Column(name = "ledger_transaction_id")
    private UUID ledgerTransactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected IdempotencyRecord() {
        // JPA
    }

    public IdempotencyRecord(String idempotencyKey, String requestFingerprint) {
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.status = STATUS_IN_PROGRESS;
        this.createdAt = Instant.now();
    }

    public void markCompleted(UUID transactionReferenceId, UUID ledgerTransactionId) {
        this.status = STATUS_COMPLETED;
        this.transactionReferenceId = transactionReferenceId;
        this.ledgerTransactionId = ledgerTransactionId;
        this.completedAt = Instant.now();
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public String getStatus() { return status; }
    public UUID getTransactionReferenceId() { return transactionReferenceId; }
    public UUID getLedgerTransactionId() { return ledgerTransactionId; }
}

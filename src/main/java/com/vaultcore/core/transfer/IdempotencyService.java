package com.vaultcore.core.transfer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages idempotency-key lifecycle in its own committed transactions ({@code REQUIRES_NEW}) so the
 * reservation is durable independent of the transfer's own transaction, and the UNIQUE constraint
 * can act as the concurrency arbiter.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * Reserves the key by inserting an {@code IN_PROGRESS} row. {@code saveAndFlush} forces the
     * UNIQUE-index check to fire now, so a concurrent duplicate surfaces as a
     * {@code DataIntegrityViolationException} to the caller rather than being deferred.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(String idempotencyKey, String requestFingerprint) {
        repository.saveAndFlush(new IdempotencyRecord(idempotencyKey, requestFingerprint));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String idempotencyKey, TransferResponseDTO response) {
        IdempotencyRecord record = repository.findByIdempotencyKey(idempotencyKey)
            .orElseThrow(() -> new IllegalStateException(
                "Idempotency reservation vanished for key: " + idempotencyKey));
        record.markCompleted(response.transactionReferenceId(), response.ledgerTransactionId());
        repository.save(record);
    }

    /**
     * Releases an uncompleted reservation so the key can be reused after a recoverable failure
     * (e.g. a fraud challenge). Completed reservations are left intact so replays keep working.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String idempotencyKey) {
        repository.findByIdempotencyKey(idempotencyKey).ifPresent(record -> {
            if (!record.isCompleted()) {
                repository.delete(record);
            }
        });
    }
}

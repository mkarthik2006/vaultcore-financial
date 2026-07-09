package com.vaultcore.core.transfer;

/**
 * Raised when an Idempotency-Key is replayed with a different request payload, or while an earlier
 * request carrying the same key is still in progress. Maps to HTTP 409 Conflict.
 */
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}

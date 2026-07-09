package com.vaultcore.core.fraud;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a transfer at/above the fraud threshold needs a 2FA challenge satisfied before it can
 * proceed. Carries the challenge identifier and expiry so the client can prompt for the code, call
 * the verify endpoint, and resubmit the transfer with the {@code X-Fraud-Challenge-Id} header.
 */
public class FraudChallengeRequiredException extends RuntimeException {

    private final String channel;
    private final UUID challengeId;
    private final Instant expiresAt;

    public FraudChallengeRequiredException(String message, String channel,
                                           UUID challengeId, Instant expiresAt) {
        super(message);
        this.channel = channel;
        this.challengeId = challengeId;
        this.expiresAt = expiresAt;
    }

    public String getChannel() {
        return channel;
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}

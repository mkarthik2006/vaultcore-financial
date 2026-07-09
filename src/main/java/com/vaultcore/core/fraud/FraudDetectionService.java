package com.vaultcore.core.fraud;

import com.vaultcore.core.transfer.TransferRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Threshold-based fraud gate. Transfers below the threshold pass. At/above it, the caller must
 * present a VERIFIED, matching, unexpired challenge (via {@code X-Fraud-Challenge-Id}); otherwise a
 * fresh challenge is issued and delivered and {@link FraudChallengeRequiredException} is thrown so
 * the customer can complete 2FA and resubmit.
 */
@Service
public class FraudDetectionService {

    private final BigDecimal threshold;
    private final boolean challengeEnabled;
    private final String channel;
    private final FraudChallengeService challengeService;

    public FraudDetectionService(@Value("${app.fraud.threshold}") BigDecimal threshold,
                                 @Value("${app.fraud.challenge-enabled:true}") boolean challengeEnabled,
                                 @Value("${app.fraud.challenge-channel:mock-sms}") String channel,
                                 FraudChallengeService challengeService) {
        this.threshold = threshold;
        this.challengeEnabled = challengeEnabled;
        this.channel = channel;
        this.challengeService = challengeService;
    }

    /**
     * @param request      the transfer intent (may be null/invalid — bean validation handles that
     *                     downstream; this method no-ops defensively)
     * @param challengeId  optional id of a previously-verified challenge supplied by the client
     */
    public void assertTransferAllowed(TransferRequestDTO request, String challengeId) {
        if (!challengeEnabled || request == null || request.amount() == null) {
            return;
        }
        if (request.amount().compareTo(threshold) < 0) {
            return;
        }

        // At/above threshold: a verified challenge may unlock this specific transfer.
        if (challengeId != null && !challengeId.isBlank()) {
            try {
                UUID id = UUID.fromString(challengeId.trim());
                if (challengeService.consumeIfVerified(id, request)) {
                    return;
                }
            } catch (IllegalArgumentException malformedId) {
                // fall through and issue a fresh challenge
            }
        }

        FraudChallenge challenge = challengeService.issue(request, channel);
        throw new FraudChallengeRequiredException(
            "Transfer exceeds fraud threshold; 2FA challenge required.",
            channel,
            challenge.getId(),
            challenge.getExpiresAt());
    }
}

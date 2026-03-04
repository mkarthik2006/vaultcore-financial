package com.vaultcore.core.fraud;

import com.vaultcore.core.transfer.TransferRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class FraudDetectionService {

    private final BigDecimal threshold;
    private final boolean challengeEnabled;
    private final String channel;
    private final FraudNotificationService notificationService;

    public FraudDetectionService(@Value("${app.fraud.threshold}") BigDecimal threshold,
                                 @Value("${app.fraud.challenge-enabled:true}") boolean challengeEnabled,
                                 @Value("${app.fraud.challenge-channel:mock-sms}") String channel,
                                 FraudNotificationService notificationService) {
        this.threshold = threshold;
        this.challengeEnabled = challengeEnabled;
        this.channel = channel;
        this.notificationService = notificationService;
    }

    public void assertTransferAllowed(TransferRequestDTO request) {
        if (request.amount().compareTo(threshold) >= 0 && challengeEnabled) {
            notificationService.sendChallenge(request.fromAccount(), channel);
            throw new FraudChallengeRequiredException(
                "Transfer exceeds fraud threshold; 2FA challenge required.",
                channel
            );
        }
    }
}
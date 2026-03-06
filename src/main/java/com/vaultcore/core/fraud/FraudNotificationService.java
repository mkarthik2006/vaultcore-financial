package com.vaultcore.core.fraud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FraudNotificationService {

    private static final Logger log = LoggerFactory.getLogger(FraudNotificationService.class);

    public void sendChallenge(String accountNumber, String channel) {
        log.warn("2FA challenge triggered for account={} via {}", accountNumber, channel);
    }
}
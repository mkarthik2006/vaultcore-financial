package com.vaultcore.core.fraud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Mock 2FA delivery channel. In production this would integrate an SMS/email provider; here it logs
 * the code so the flow can be exercised end-to-end in development and tests.
 */
@Service
public class FraudNotificationService {

    private static final Logger log = LoggerFactory.getLogger(FraudNotificationService.class);

    public void sendChallenge(String accountNumber, String channel, String code) {
        // NOTE: logging the plaintext code is acceptable ONLY for the mock channel used in dev/test.
        log.warn("2FA challenge delivered for account={} via {} (mock code={})",
            accountNumber, channel, code);
    }
}

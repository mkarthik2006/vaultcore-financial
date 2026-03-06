package com.vaultcore.core.transfer;

import org.springframework.stereotype.Component;

@Component
public class TransferRetryPolicy {

    // 100 concurrent SERIALIZABLE txns may need more retries under SSI.
    public int maxAttempts() {
        return 30;
    }

    // base delay in ms
    public long baseDelayMs() {
        return 10L;
    }

    // cap delay in ms to keep tests bounded
    public long maxDelayMs() {
        return 250L;
    }
}
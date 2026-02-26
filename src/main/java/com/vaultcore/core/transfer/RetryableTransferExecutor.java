package com.vaultcore.core.transfer;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RetryableTransferExecutor {

    private final TransferRetryPolicy policy;

    public RetryableTransferExecutor(TransferRetryPolicy policy) {
        this.policy = policy;
    }

    public <T> T executeWithRetry(RetryableWork<T> work) {
        int attempt = 0;

        while (true) {
            attempt++;
            try {
                return work.run();
            } catch (RuntimeException ex) {
                if (attempt >= policy.maxAttempts() || !isRetryable(ex)) {
                    throw ex;
                }
                backoffWithJitter(attempt);
            }
        }
    }

    private boolean isRetryable(RuntimeException ex) {
        if (ex instanceof CannotAcquireLockException) return true;
        if (ex instanceof TransientDataAccessException) return true;

        Throwable t = ex;
        while (t != null) {
            if (t instanceof java.sql.SQLException sqlEx && "40001".equals(sqlEx.getSQLState())) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private void backoffWithJitter(int attempt) {
        long base = policy.baseDelayMs();
        long cap = policy.maxDelayMs();

        long exp = base * (1L << Math.min(attempt - 1, 10)); // prevent overflow
        long sleepMax = Math.min(cap, exp);

        long sleep = ThreadLocalRandom.current().nextLong(0, sleepMax + 1);

        try {
            Thread.sleep(sleep);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    public interface RetryableWork<T> {
        T run();
    }
}
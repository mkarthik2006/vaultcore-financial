package com.vaultcore.core.ledger;

import com.vaultcore.core.account.AccountRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Service
public class LedgerQueryService {

    private final LedgerRepository ledgerRepository;
    private final AccountRepository accountRepository;
    private final ExecutorService virtualThreadExecutor;

    public LedgerQueryService(LedgerRepository ledgerRepository,
                              AccountRepository accountRepository,
                              ExecutorService virtualThreadExecutor) {
        this.ledgerRepository = ledgerRepository;
        this.accountRepository = accountRepository;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    @Cacheable(cacheNames = "balances", key = "#accountNumber + ':' + #currency")
    public BigDecimal getBalance(String accountNumber, String currency) {
        var account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));

        try {
            Future<BigDecimal> future = virtualThreadExecutor.submit(
                () -> ledgerRepository.getBalance(account.getId(), currency)
            );
            return future.get();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to fetch balance", ex);
        }
    }
}
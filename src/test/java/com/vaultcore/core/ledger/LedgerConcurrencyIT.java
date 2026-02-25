package com.vaultcore.core.ledger;

import com.vaultcore.config.IntegrationTestBase;
import com.vaultcore.security.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@Import(TestSecurityConfig.class)
class LedgerConcurrencyIT extends IntegrationTestBase {

    @Autowired
    private LedgerService ledgerService;

    @Test
    void serializableConcurrencyShouldSucceed() throws Exception {
        int threads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                UUID txnId = UUID.randomUUID();
                LedgerEntry debit = new LedgerEntry(
                    txnId, UUID.randomUUID(),
                    LedgerEntry.EntryType.DEBIT,
                    new BigDecimal("100.00"),
                    "USD",
                    "debit"
                );
                LedgerEntry credit = new LedgerEntry(
                    txnId, UUID.randomUUID(),
                    LedgerEntry.EntryType.CREDIT,
                    new BigDecimal("100.00"),
                    "USD",
                    "credit"
                );
                ledgerService.recordTransaction(txnId, List.of(debit, credit));
                return null;
            });
        }

        assertDoesNotThrow(() -> executor.invokeAll(tasks, 60, TimeUnit.SECONDS));
        executor.shutdown();
    }
}
package com.vaultcore.core.transfer;

import com.vaultcore.config.IntegrationTestBase;
import com.vaultcore.core.account.Account;
import com.vaultcore.core.account.AccountRepository;
import com.vaultcore.core.ledger.LedgerEntry;
import com.vaultcore.core.ledger.LedgerRepository;
import com.vaultcore.core.ledger.LedgerService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestSecurityConfig.class)
class TransferServiceConcurrencyIT extends IntegrationTestBase {

    private static final int THREADS = 100;
    private static final String CURRENCY = "USD";
    private static final BigDecimal INITIAL_FUNDING = new BigDecimal("20000.00");
    private static final BigDecimal AMOUNT_PER_TRANSFER = new BigDecimal("200.00");

    private static final String SENDER_ACCOUNT_NO = "A001";
    private static final String RECEIVER_ACCOUNT_NO = "A002";
    private static final String CLEARING_ACCOUNT_NO = "SYS_CLEARING";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private TransferService transferService; // will be created in Layer 5

    @Test
    void serializableConcurrentTransfers_shouldKeepFinalBalanceCorrectAndNonNegative() throws Exception {
        // Arrange: accounts
        Account sender = accountRepository.save(new Account(SENDER_ACCOUNT_NO, CURRENCY));
        Account receiver = accountRepository.save(new Account(RECEIVER_ACCOUNT_NO, CURRENCY));
        Account clearing = accountRepository.save(new Account(CLEARING_ACCOUNT_NO, CURRENCY));

        // Seed sender with funds via a valid double-entry transaction (clearing DEBIT, sender CREDIT)
        UUID seedTxnId = UUID.randomUUID();
        LedgerEntry clearingDebit = new LedgerEntry(
            seedTxnId,
            clearing.getId(),
            LedgerEntry.EntryType.DEBIT,
            INITIAL_FUNDING,
            CURRENCY,
            "seed-funding debit from clearing"
        );
        LedgerEntry senderCredit = new LedgerEntry(
            seedTxnId,
            sender.getId(),
            LedgerEntry.EntryType.CREDIT,
            INITIAL_FUNDING,
            CURRENCY,
            "seed-funding credit to sender"
        );
        ledgerService.recordTransaction(seedTxnId, List.of(clearingDebit, senderCredit));

        // Pre-assert: sender funded
        BigDecimal senderBefore = ledgerRepository.getBalance(sender.getId(), CURRENCY);
        assertEquals(0, INITIAL_FUNDING.compareTo(senderBefore));

        // Act: run 100 parallel transfers A001 -> A002
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<Void>> tasks = new ArrayList<>();

            for (int i = 0; i < THREADS; i++) {
                tasks.add(() -> {
                    TransferRequestDTO req = new TransferRequestDTO(
                        SENDER_ACCOUNT_NO,
                        RECEIVER_ACCOUNT_NO,
                        AMOUNT_PER_TRANSFER,
                        CURRENCY
                    );
                    transferService.transfer(req);
                    return null;
                });
            }

            List<Future<Void>> futures = executor.invokeAll(tasks, 60, TimeUnit.SECONDS);

            // Ensure no task failed silently
            for (Future<Void> f : futures) {
                f.get(); // will throw if the task failed
            }
        }

        // Assert: final balances
        BigDecimal senderAfter = ledgerRepository.getBalance(sender.getId(), CURRENCY);
        BigDecimal receiverAfter = ledgerRepository.getBalance(receiver.getId(), CURRENCY);

        BigDecimal expectedSender = INITIAL_FUNDING.subtract(AMOUNT_PER_TRANSFER.multiply(new BigDecimal(THREADS)));
        BigDecimal expectedReceiver = AMOUNT_PER_TRANSFER.multiply(new BigDecimal(THREADS));

        assertEquals(0, expectedSender.compareTo(senderAfter));
        assertTrue(senderAfter.compareTo(BigDecimal.ZERO) >= 0);

        assertEquals(0, expectedReceiver.compareTo(receiverAfter));

        // Recommended conservation check (only valid because we control test setup)
        assertEquals(0, INITIAL_FUNDING.compareTo(senderAfter.add(receiverAfter)));
    }
}
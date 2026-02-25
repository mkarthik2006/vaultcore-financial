package com.vaultcore.core.ledger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class LedgerServiceTest {

    @Test
    void rejectsSingleEntry() {
        LedgerRepository repo = null;
        LedgerService service = new LedgerService(repo);

        UUID txnId = UUID.randomUUID();
        LedgerEntry entry = new LedgerEntry(
            txnId, UUID.randomUUID(),
            LedgerEntry.EntryType.DEBIT,
            new BigDecimal("100.00"),
            "USD",
            "single"
        );

        assertThrows(IllegalArgumentException.class, () ->
            service.recordTransaction(txnId, List.of(entry))
        );
    }

    @Test
    void rejectsImbalancedEntries() {
        LedgerRepository repo = null;
        LedgerService service = new LedgerService(repo);

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
            new BigDecimal("50.00"),
            "USD",
            "credit"
        );

        assertThrows(IllegalArgumentException.class, () ->
            service.recordTransaction(txnId, List.of(debit, credit))
        );
    }
}
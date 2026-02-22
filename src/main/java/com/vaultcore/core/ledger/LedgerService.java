package com.vaultcore.core.ledger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerService {

    private final LedgerRepository ledgerRepository;

    public LedgerService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void recordTransaction(UUID transactionId, List<LedgerEntry> entries) {
        validateDoubleEntry(transactionId, entries);
        ledgerRepository.saveAll(entries);
    }

    private void validateDoubleEntry(UUID transactionId, List<LedgerEntry> entries) {
        if (entries == null || entries.size() < 2) {
            throw new IllegalArgumentException("Double-entry requires at least 2 entries.");
        }

        BigDecimal debitSum = BigDecimal.ZERO;
        BigDecimal creditSum = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            if (!transactionId.equals(entry.getTransactionId())) {
                throw new IllegalArgumentException("All ledger entries must share transactionId.");
            }
            if (entry.getEntryType() == LedgerEntry.EntryType.DEBIT) {
                debitSum = debitSum.add(entry.getAmount());
            } else if (entry.getEntryType() == LedgerEntry.EntryType.CREDIT) {
                creditSum = creditSum.add(entry.getAmount());
            }
        }

        if (debitSum.compareTo(creditSum) != 0) {
            throw new IllegalArgumentException("Double-entry imbalance: debits != credits.");
        }
    }
}
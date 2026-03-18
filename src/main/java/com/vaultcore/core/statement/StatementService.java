package com.vaultcore.core.statement;

import com.vaultcore.core.account.Account;
import com.vaultcore.core.account.AccountRepository;
import com.vaultcore.core.ledger.LedgerEntry;
import com.vaultcore.core.ledger.LedgerRepository;
import com.vaultcore.core.transaction.TransactionReferenceRepository;
import com.vaultcore.user.UserEntity;
import com.vaultcore.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class StatementService {

    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;
    private final TransactionReferenceRepository transactionReferenceRepository;
    private final UserRepository userRepository;
    private final StatementPdfRenderer pdfRenderer;

    public StatementService(AccountRepository accountRepository,
                            LedgerRepository ledgerRepository,
                            TransactionReferenceRepository transactionReferenceRepository,
                            UserRepository userRepository,
                            StatementPdfRenderer pdfRenderer) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.transactionReferenceRepository = transactionReferenceRepository;
        this.userRepository = userRepository;
        this.pdfRenderer = pdfRenderer;
    }

    @Transactional(readOnly = true)
    public byte[] generateMonthlyStatement(String accountNumber, YearMonth month, String username) {
        UserEntity user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Account account = accountRepository.findByAccountNumberAndOwner_Id(accountNumber, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Account not found for user"));

        Instant start = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<LedgerEntry> entries = ledgerRepository
            .findByAccountIdAndCreatedAtBetweenOrderByCreatedAtAsc(account.getId(), start, end);

        BigDecimal opening = ledgerRepository.getBalanceUpTo(account.getId(), account.getCurrency(), start);
        BigDecimal closing = opening;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (LedgerEntry e : entries) {
            if (e.getEntryType() == LedgerEntry.EntryType.DEBIT) {
                totalDebits = totalDebits.add(e.getAmount());
                closing = closing.subtract(e.getAmount());
            } else {
                totalCredits = totalCredits.add(e.getAmount());
                closing = closing.add(e.getAmount());
            }
        }

        MonthlyStatement statement = new MonthlyStatement(
            account.getAccountNumber(),
            account.getCurrency(),
            month,
            opening,
            closing,
            totalDebits,
            totalCredits,
            entries
        );

        return pdfRenderer.render(statement);
    }

    // Uses TransactionReferenceRepository to assert reference exists when present in description
    UUID resolveReferenceId(String description) {
        if (description == null) return null;
        int idx = description.indexOf("ref=");
        if (idx < 0) return null;
        try {
            String raw = description.substring(idx + 4).replace(")", "").trim();
            UUID id = UUID.fromString(raw);
            return transactionReferenceRepository.findById(id).map(r -> id).orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }
}
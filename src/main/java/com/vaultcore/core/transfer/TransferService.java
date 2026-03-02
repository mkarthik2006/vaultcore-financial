package com.vaultcore.core.transfer;

import com.vaultcore.core.account.Account;
import com.vaultcore.core.account.AccountRepository;
import com.vaultcore.core.ledger.LedgerEntry;
import com.vaultcore.core.ledger.LedgerRepository;
import com.vaultcore.core.ledger.LedgerService;
import com.vaultcore.core.transaction.TransactionReference;
import com.vaultcore.core.transaction.TransactionReferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionReferenceRepository transactionReferenceRepository;
    private final LedgerRepository ledgerRepository;
    private final LedgerService ledgerService;

    private final TransactionTemplate transactionTemplate;
    private final RetryableTransferExecutor retryableTransferExecutor;

    public TransferService(AccountRepository accountRepository,
                           TransactionReferenceRepository transactionReferenceRepository,
                           LedgerRepository ledgerRepository,
                           LedgerService ledgerService,
                           TransactionTemplate transactionTemplate,
                           RetryableTransferExecutor retryableTransferExecutor) {
        this.accountRepository = accountRepository;
        this.transactionReferenceRepository = transactionReferenceRepository;
        this.ledgerRepository = ledgerRepository;
        this.ledgerService = ledgerService;
        this.transactionTemplate = transactionTemplate;
        this.retryableTransferExecutor = retryableTransferExecutor;
    }

    /**
     * Week 2 requirement:
     * - SERIALIZABLE transaction
     * - PESSIMISTIC_WRITE locking
     * - double-entry ledger per transfer
     * - final balance correct + non-negative under 100 threads
     *
     * Postgres SERIALIZABLE can abort with SQLSTATE 40001 (often wrapped by Spring).
     * Correct handling is retry in a brand-new transaction.
     */
    public TransferResponseDTO transfer(TransferRequestDTO request) {
        validateRequest(request);

        return retryableTransferExecutor.executeWithRetry(() -> {
            TransactionTemplate tx = new TransactionTemplate(transactionTemplate.getTransactionManager());
            tx.setIsolationLevel(Isolation.SERIALIZABLE.value());
            return tx.execute(status -> doTransferInSerializableTx(request));
        });
    }

    private TransferResponseDTO doTransferInSerializableTx(TransferRequestDTO request) {
        // Lock rows in deterministic order to reduce deadlocks
        String n1 = request.fromAccount();
        String n2 = request.toAccount();

        Account firstLocked;
        Account secondLocked;

        if (n1.compareTo(n2) <= 0) {
            firstLocked = lockAccountByNumber(n1);
            secondLocked = lockAccountByNumber(n2);
        } else {
            firstLocked = lockAccountByNumber(n2);
            secondLocked = lockAccountByNumber(n1);
        }

        Account from = firstLocked.getAccountNumber().equals(request.fromAccount()) ? firstLocked : secondLocked;
        Account to = (from == firstLocked) ? secondLocked : firstLocked;

        enforceCurrency(request, from, to);

        BigDecimal fromBalance = ledgerRepository.getBalance(from.getId(), request.currency());
        if (fromBalance.compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException(
                "Insufficient funds for account " + from.getAccountNumber() +
                    " balance=" + fromBalance + " amount=" + request.amount()
            );
        }

        TransactionReference ref = transactionReferenceRepository.save(TransactionReference.now());
        UUID ledgerTxnId = UUID.randomUUID();

        LedgerEntry debit = new LedgerEntry(
            ledgerTxnId,
            from.getId(),
            LedgerEntry.EntryType.DEBIT,
            request.amount(),
            request.currency(),
            "transfer debit to " + to.getAccountNumber() + " (ref=" + ref.getId() + ")"
        );

        LedgerEntry credit = new LedgerEntry(
            ledgerTxnId,
            to.getId(),
            LedgerEntry.EntryType.CREDIT,
            request.amount(),
            request.currency(),
            "transfer credit from " + from.getAccountNumber() + " (ref=" + ref.getId() + ")"
        );

        ledgerService.recordTransaction(ledgerTxnId, List.of(debit, credit));

        return new TransferResponseDTO(ref.getId(), ledgerTxnId);
    }

    private Account lockAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }

    private void enforceCurrency(TransferRequestDTO request, Account from, Account to) {
        String c = request.currency();
        if (!c.equals(from.getCurrency())) {
            throw new CurrencyMismatchException("Currency mismatch: request=" + c + " fromAccount=" + from.getCurrency());
        }
        if (!c.equals(to.getCurrency())) {
            throw new CurrencyMismatchException("Currency mismatch: request=" + c + " toAccount=" + to.getCurrency());
        }
    }

    private void validateRequest(TransferRequestDTO request) {
        if (request == null) throw new IllegalArgumentException("TransferRequestDTO must not be null");

        if (request.fromAccount() == null || request.fromAccount().isBlank())
            throw new IllegalArgumentException("fromAccount must not be blank");

        if (request.toAccount() == null || request.toAccount().isBlank())
            throw new IllegalArgumentException("toAccount must not be blank");

        if (request.fromAccount().equals(request.toAccount()))
            throw new IllegalArgumentException("fromAccount and toAccount must be different");

        if (request.currency() == null || request.currency().isBlank())
            throw new IllegalArgumentException("currency must not be blank");

        if (request.currency().length() != 3)
            throw new IllegalArgumentException("currency must be length 3");

        if (request.amount() == null)
            throw new IllegalArgumentException("amount must not be null");

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("amount must be > 0");
    }
}
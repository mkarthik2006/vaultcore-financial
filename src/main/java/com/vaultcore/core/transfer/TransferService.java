package com.vaultcore.core.transfer;

import com.vaultcore.core.account.Account;
import com.vaultcore.core.account.AccountRepository;
import com.vaultcore.core.ledger.LedgerEntry;
import com.vaultcore.core.ledger.LedgerRepository;
import com.vaultcore.core.ledger.LedgerService;
import com.vaultcore.core.transaction.TransactionReference;
import com.vaultcore.core.transaction.TransactionReferenceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionReferenceRepository transactionReferenceRepository;
    private final LedgerRepository ledgerRepository;
    private final LedgerService ledgerService;
    private final RetryableTransferExecutor retryableTransferExecutor;
    private final IdempotencyService idempotencyService;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final TransferService self;

    public TransferService(AccountRepository accountRepository,
                           TransactionReferenceRepository transactionReferenceRepository,
                           LedgerRepository ledgerRepository,
                           LedgerService ledgerService,
                           RetryableTransferExecutor retryableTransferExecutor,
                           IdempotencyService idempotencyService,
                           IdempotencyRecordRepository idempotencyRecordRepository,
                           @Lazy TransferService self) {
        this.accountRepository = accountRepository;
        this.transactionReferenceRepository = transactionReferenceRepository;
        this.ledgerRepository = ledgerRepository;
        this.ledgerService = ledgerService;
        this.retryableTransferExecutor = retryableTransferExecutor;
        this.idempotencyService = idempotencyService;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.self = self;
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
    /**
     * Execution primitive for a single transfer. Fraud detection is applied here by
     * {@code FraudDetectionAspect} (AOP {@code @Before} on this exact join point), so it runs once
     * per real execution and is not repeated across serialization retries or idempotent replays.
     */
    public TransferResponseDTO transfer(TransferRequestDTO request) {
        validateRequest(request);
        return retryableTransferExecutor.executeWithRetry(() -> self.transferInSerializableTx(request));
    }

    /**
     * Idempotent entry point. When an {@code idempotencyKey} is supplied, at most one transfer is
     * executed per key regardless of how many times the request is (re)submitted; repeats replay the
     * original result. The key is reserved via a UNIQUE constraint <em>before</em> execution so two
     * concurrent duplicates cannot both post — the loser observes the winner's reservation.
     *
     * @throws IdempotencyConflictException if the key is reused with a different payload, or an
     *                                       earlier request with the same key is still in progress
     */
    public TransferResponseDTO transfer(TransferRequestDTO request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            // No key supplied: execute via the proxy so the fraud aspect still applies.
            return self.transfer(request);
        }

        String fingerprint = fingerprint(request);

        var existing = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return resolveExisting(existing.get(), fingerprint);
        }

        try {
            idempotencyService.reserve(idempotencyKey, fingerprint);
        } catch (DataIntegrityViolationException concurrentDuplicate) {
            // A concurrent request won the reservation race; defer to its outcome.
            IdempotencyRecord record = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IdempotencyConflictException(
                    "Idempotency-Key is being processed by a concurrent request"));
            return resolveExisting(record, fingerprint);
        }

        // We own execution for this key. Route through the proxy so fraud detection applies; if it
        // (or anything else) fails before completion, release the reservation so the caller can
        // legitimately retry — e.g. after satisfying a fraud challenge.
        TransferResponseDTO response;
        try {
            response = self.transfer(request);
        } catch (RuntimeException ex) {
            idempotencyService.release(idempotencyKey);
            throw ex;
        }
        idempotencyService.complete(idempotencyKey, response);
        return response;
    }

    private TransferResponseDTO resolveExisting(IdempotencyRecord record, String fingerprint) {
        if (!record.getRequestFingerprint().equals(fingerprint)) {
            throw new IdempotencyConflictException(
                "Idempotency-Key was already used with a different request");
        }
        if (record.isCompleted()) {
            return new TransferResponseDTO(
                record.getTransactionReferenceId(),
                record.getLedgerTransactionId());
        }
        throw new IdempotencyConflictException(
            "A request with this Idempotency-Key is still being processed");
    }

    private String fingerprint(TransferRequestDTO request) {
        String canonical = String.join("|",
            request.fromAccount(),
            request.toAccount(),
            request.amount().stripTrailingZeros().toPlainString(),
            request.currency().toUpperCase());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @CacheEvict(cacheNames = "balances", allEntries = true)
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public TransferResponseDTO transferInSerializableTx(TransferRequestDTO request) {
        return doTransferInSerializableTx(request);
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
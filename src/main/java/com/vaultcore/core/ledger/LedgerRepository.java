package com.vaultcore.core.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByTransactionId(UUID transactionId);
    List<LedgerEntry> findByAccountId(UUID accountId);

    List<LedgerEntry> findByAccountIdAndCreatedAtBetweenOrderByCreatedAtAsc(
        UUID accountId, Instant start, Instant end);

    @Query("""
        select
          coalesce(sum(case when e.entryType = 'CREDIT' then e.amount else 0 end), 0) -
          coalesce(sum(case when e.entryType = 'DEBIT' then e.amount else 0 end), 0)
        from LedgerEntry e
        where e.accountId = :accountId
          and e.currency = :currency
        """)
    BigDecimal getBalance(@Param("accountId") UUID accountId, @Param("currency") String currency);

    @Query("""
        select
          coalesce(sum(case when e.entryType = 'CREDIT' then e.amount else 0 end), 0) -
          coalesce(sum(case when e.entryType = 'DEBIT' then e.amount else 0 end), 0)
        from LedgerEntry e
        where e.accountId = :accountId
          and e.currency = :currency
          and e.createdAt < :before
        """)
    BigDecimal getBalanceUpTo(@Param("accountId") UUID accountId,
                              @Param("currency") String currency,
                              @Param("before") Instant before);
}
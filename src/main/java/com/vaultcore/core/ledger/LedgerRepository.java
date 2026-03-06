package com.vaultcore.core.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByTransactionId(UUID transactionId);
    List<LedgerEntry> findByAccountId(UUID accountId);
  @Query("""
        select coalesce(sum(
            case
                when e.entryType = com.vaultcore.core.ledger.LedgerEntry.EntryType.CREDIT then e.amount
                else -e.amount
            end
        ), 0)
        from LedgerEntry e
        where e.accountId = :accountId
          and e.currency = :currency
        """)
    BigDecimal getBalance(@Param("accountId") UUID accountId, @Param("currency") String currency);
}
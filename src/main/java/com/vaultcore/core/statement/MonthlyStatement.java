package com.vaultcore.core.statement;

import com.vaultcore.core.ledger.LedgerEntry;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record MonthlyStatement(
    String accountNumber,
    String currency,
    YearMonth month,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    BigDecimal totalDebits,
    BigDecimal totalCredits,
    List<LedgerEntry> entries
) {
}
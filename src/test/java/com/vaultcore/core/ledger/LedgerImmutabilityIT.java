package com.vaultcore.core.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class LedgerImmutabilityIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void updateShouldFail() {
        UUID id = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, currency)
            VALUES (?, ?, ?, 'DEBIT', 100.00, 'USD')
            """, id, txnId, accountId);

        assertThrows(DataIntegrityViolationException.class, () ->
            jdbcTemplate.update("UPDATE ledger_entries SET amount = 200.00 WHERE id = ?", id)
        );
    }

    @Test
    void deleteShouldFail() {
        UUID id = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, currency)
            VALUES (?, ?, ?, 'DEBIT', 100.00, 'USD')
            """, id, txnId, accountId);

        assertThrows(DataIntegrityViolationException.class, () ->
            jdbcTemplate.update("DELETE FROM ledger_entries WHERE id = ?", id)
        );
    }
}
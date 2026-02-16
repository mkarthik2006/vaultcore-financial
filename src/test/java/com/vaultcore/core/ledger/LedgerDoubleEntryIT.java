package com.vaultcore.core.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class LedgerDoubleEntryIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void singleEntryShouldFail() {
        UUID txnId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO ledger_entries (transaction_id, account_id, entry_type, amount, currency)
            VALUES (?, ?, 'DEBIT', 100.00, 'USD')
            """, txnId, accountId);

        assertThrows(Exception.class, () ->
            jdbcTemplate.execute("SET CONSTRAINTS ALL IMMEDIATE")
        );
    }

    @Test
    void mismatchShouldFail() {
        UUID txnId = UUID.randomUUID();
        UUID accountId1 = UUID.randomUUID();
        UUID accountId2 = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO ledger_entries (transaction_id, account_id, entry_type, amount, currency)
            VALUES (?, ?, 'DEBIT', 100.00, 'USD')
            """, txnId, accountId1);

        jdbcTemplate.update("""
            INSERT INTO ledger_entries (transaction_id, account_id, entry_type, amount, currency)
            VALUES (?, ?, 'CREDIT', 50.00, 'USD')
            """, txnId, accountId2);

        assertThrows(Exception.class, () ->
            jdbcTemplate.execute("SET CONSTRAINTS ALL IMMEDIATE")
        );
    }

    @Test
    void validDoubleEntrySucceeds() {
        UUID txnId = UUID.randomUUID();
        UUID accountId1 = UUID.randomUUID();
        UUID accountId2 = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO ledger_entries (transaction_id, account_id, entry_type, amount, currency)
            VALUES (?, ?, 'DEBIT', 100.00, 'USD')
            """, txnId, accountId1);

        jdbcTemplate.update("""
            INSERT INTO ledger_entries (transaction_id, account_id, entry_type, amount, currency)
            VALUES (?, ?, 'CREDIT', 100.00, 'USD')
            """, txnId, accountId2);

        assertDoesNotThrow(() ->
            jdbcTemplate.execute("SET CONSTRAINTS ALL IMMEDIATE")
        );
    }
}
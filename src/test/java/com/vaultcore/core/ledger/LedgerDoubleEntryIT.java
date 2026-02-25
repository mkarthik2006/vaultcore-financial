package com.vaultcore.core.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import com.vaultcore.config.IntegrationTestBase;
import com.vaultcore.security.TestSecurityConfig;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestSecurityConfig.class)
class LedgerDoubleEntryIT extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
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
    @Transactional
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
    @Transactional
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
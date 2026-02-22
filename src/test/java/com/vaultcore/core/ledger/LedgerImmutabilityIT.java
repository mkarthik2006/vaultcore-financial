package com.vaultcore.core.ledger;

import com.vaultcore.config.IntegrationTestBase;
import com.vaultcore.security.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestSecurityConfig.class)
class LedgerImmutabilityIT extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void updateShouldFail() {
        UUID txnId = UUID.randomUUID();
        UUID debitId = UUID.randomUUID();
        UUID creditId = UUID.randomUUID();
        UUID accountId1 = UUID.randomUUID();
        UUID accountId2 = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, currency)
            VALUES (?, ?, ?, 'DEBIT', 100.00, 'USD')
            """, debitId, txnId, accountId1);

        jdbcTemplate.update("""
            INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, currency)
            VALUES (?, ?, ?, 'CREDIT', 100.00, 'USD')
            """, creditId, txnId, accountId2);

        jdbcTemplate.execute("SET CONSTRAINTS ALL IMMEDIATE");

        assertThrows(DataAccessException.class, () ->
    jdbcTemplate.update("UPDATE ledger_entries SET amount = 200.00 WHERE id = ?", debitId)
);
    }

    @Test
    @Transactional
    void deleteShouldFail() {
        UUID txnId = UUID.randomUUID();
        UUID debitId = UUID.randomUUID();
        UUID creditId = UUID.randomUUID();
        UUID accountId1 = UUID.randomUUID();
        UUID accountId2 = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, currency)
            VALUES (?, ?, ?, 'DEBIT', 100.00, 'USD')
            """, debitId, txnId, accountId1);

        jdbcTemplate.update("""
            INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, currency)
            VALUES (?, ?, ?, 'CREDIT', 100.00, 'USD')
            """, creditId, txnId, accountId2);

        jdbcTemplate.execute("SET CONSTRAINTS ALL IMMEDIATE");

        assertThrows(DataAccessException.class, () ->
    jdbcTemplate.update("DELETE FROM ledger_entries WHERE id = ?", debitId)
);
    }
}
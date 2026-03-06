package com.vaultcore.core.statement;

import com.vaultcore.config.IntegrationTestBase;
import com.vaultcore.core.account.Account;
import com.vaultcore.core.account.AccountRepository;
import com.vaultcore.core.ledger.LedgerEntry;
import com.vaultcore.core.ledger.LedgerService;
import com.vaultcore.security.TestSecurityConfig;
import com.vaultcore.user.UserEntity;
import com.vaultcore.user.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestSecurityConfig.class)
class StatementServiceTest extends IntegrationTestBase {

    @Autowired
    private StatementService statementService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerService ledgerService;

    @Test
    void generatesMonthlyStatementPdf() throws Exception {
        UserEntity user = userRepository.save(new UserEntity(
            UUID.randomUUID(), "statement@vaultcore.com", "statementUser", "hash", true, "USER"
        ));

        Account account = accountRepository.save(new Account("ACC-001", "USD", user));

        UUID txnId = UUID.randomUUID();
        LedgerEntry debit = new LedgerEntry(
            txnId, account.getId(), LedgerEntry.EntryType.DEBIT,
            new BigDecimal("25.00"), "USD", "statement debit"
        );
        LedgerEntry credit = new LedgerEntry(
            txnId, account.getId(), LedgerEntry.EntryType.CREDIT,
            new BigDecimal("25.00"), "USD", "statement credit"
        );
        ledgerService.recordTransaction(txnId, List.of(debit, credit));

        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        byte[] pdf = statementService.generateMonthlyStatement("ACC-001", month, "statementUser");

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);

        try (PDDocument doc = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("VaultCore Monthly Statement");
            assertThat(text).contains("Account: ACC-001");
            assertThat(text).contains("Total Debits");
            assertThat(text).contains("Total Credits");
        }
    }
}
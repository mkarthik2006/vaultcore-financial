package com.vaultcore.audit;

import com.vaultcore.config.IntegrationTestBase;
import com.vaultcore.core.ledger.LedgerEntry;
import com.vaultcore.core.ledger.LedgerService;
import com.vaultcore.security.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestSecurityConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class AuditAspectTest extends IntegrationTestBase {

    @Autowired
    private LedgerService ledgerService;

    @Test
    void auditLogsControllerAndServiceCalls(CapturedOutput output) {
        UUID txnId = UUID.randomUUID();
        LedgerEntry debit = new LedgerEntry(
            txnId, UUID.randomUUID(),
            LedgerEntry.EntryType.DEBIT,
            new BigDecimal("50.00"),
            "USD",
            "audit-test-debit"
        );
        LedgerEntry credit = new LedgerEntry(
            txnId, UUID.randomUUID(),
            LedgerEntry.EntryType.CREDIT,
            new BigDecimal("50.00"),
            "USD",
            "audit-test-credit"
        );

        ledgerService.recordTransaction(txnId, List.of(debit, credit));

        assertThat(output.getOut())
            .contains("AUDIT method=LedgerService.recordTransaction")
            .contains("durationMs=");
    }
}
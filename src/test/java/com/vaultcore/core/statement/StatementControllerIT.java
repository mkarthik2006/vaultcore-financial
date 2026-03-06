package com.vaultcore.core.statement;

import com.vaultcore.config.IntegrationTestBase;
import com.vaultcore.core.account.Account;
import com.vaultcore.core.account.AccountRepository;
import com.vaultcore.core.ledger.LedgerEntry;
import com.vaultcore.core.ledger.LedgerService;
import com.vaultcore.security.TestSecurityConfig;
import com.vaultcore.user.UserEntity;
import com.vaultcore.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class StatementControllerIT extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerService ledgerService;

    @Test
    void authenticatedRequestReturnsPdf() {
        UserEntity user = userRepository.save(new UserEntity(
            UUID.randomUUID(), "owner@vaultcore.com", "ownerUser", "hash", true, "USER"
        ));
        Account account = accountRepository.save(new Account("ACC-100", "USD", user));

        UUID txnId = UUID.randomUUID();
        LedgerEntry debit = new LedgerEntry(
            txnId, account.getId(), LedgerEntry.EntryType.DEBIT,
            new BigDecimal("10.00"), "USD", "controller debit"
        );
        LedgerEntry credit = new LedgerEntry(
            txnId, account.getId(), LedgerEntry.EntryType.CREDIT,
            new BigDecimal("10.00"), "USD", "controller credit"
        );
        ledgerService.recordTransaction(txnId, List.of(debit, credit));

        String month = YearMonth.now(ZoneOffset.UTC).toString();

        ResponseEntity<byte[]> response = restTemplate.exchange(
            "/api/v1/statements/monthly?accountNumber=ACC-100&month=" + month,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders("ownerUser")),
            byte[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
            .contains("statement-ACC-100-" + month + ".pdf");
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void rejectsAccessForNonOwner() {
        UserEntity owner = userRepository.save(new UserEntity(
            UUID.randomUUID(), "owner2@vaultcore.com", "ownerUser2", "hash", true, "USER"
        ));
        accountRepository.save(new Account("ACC-200", "USD", owner));

        String month = YearMonth.now(ZoneOffset.UTC).toString();

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/statements/monthly?accountNumber=ACC-200&month=" + month,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders("otherUser")),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("User not found: otherUser");
    }

    private HttpHeaders authHeaders(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(username);
        return headers;
    }
}
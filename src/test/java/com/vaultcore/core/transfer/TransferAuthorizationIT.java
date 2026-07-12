package com.vaultcore.core.transfer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultcore.config.IntegrationTestBase;
import com.vaultcore.core.account.Account;
import com.vaultcore.core.account.AccountRepository;
import com.vaultcore.core.ledger.LedgerEntry;
import com.vaultcore.core.ledger.LedgerRepository;
import com.vaultcore.core.ledger.LedgerService;
import com.vaultcore.security.TestSecurityConfig;
import com.vaultcore.user.UserEntity;
import com.vaultcore.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Object-level authorization (IDOR) and idempotency behaviour of the transfer endpoint, exercised
 * through the real security filter chain via MockMvc. The test JWT decoder maps the bearer token
 * string to {@code preferred_username}, so a bearer of "alice" authenticates as user alice.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class TransferAuthorizationIT extends IntegrationTestBase {

    private static final String CURRENCY = "USD";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private LedgerService ledgerService;
    @Autowired private LedgerRepository ledgerRepository;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void unauthenticatedTransferIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferJson("A100", "A200", "50.00")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void callerCannotTransferFromAnAccountTheyDoNotOwn() throws Exception {
        UserEntity alice = createUser("alice");
        UserEntity bob = createUser("bob");
        createOwnedAccount("ALICE1", alice);
        Account bobs = createOwnedAccount("BOB1", bob);
        fund(bobs, "5000.00");

        // Alice tries to debit Bob's account -> IDOR must be blocked with a generic 403.
        mockMvc.perform(post("/api/v1/transfers")
                .header("Authorization", "Bearer alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferJson("BOB1", "ALICE1", "100.00")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("access_denied"));
    }

    @Test
    void callerCanTransferFromTheirOwnAccount() throws Exception {
        UserEntity alice = createUser("owner1");
        UserEntity bob = createUser("owner2");
        Account from = createOwnedAccount("OWN1", alice);
        createOwnedAccount("OWN2", bob);
        fund(from, "5000.00");

        mockMvc.perform(post("/api/v1/transfers")
                .header("Authorization", "Bearer owner1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferJson("OWN1", "OWN2", "200.00")))
            .andExpect(status().isCreated());

        assertEquals(0, new BigDecimal("4800.00").compareTo(
            ledgerRepository.getBalance(from.getId(), CURRENCY)));
    }

    @Test
    void repeatedIdempotencyKeyExecutesTransferOnlyOnce() throws Exception {
        UserEntity alice = createUser("idem1");
        UserEntity bob = createUser("idem2");
        Account from = createOwnedAccount("IDEM1", alice);
        createOwnedAccount("IDEM2", bob);
        fund(from, "5000.00");

        String key = UUID.randomUUID().toString();

        String firstBody = mockMvc.perform(post("/api/v1/transfers")
                .header("Authorization", "Bearer idem1")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferJson("IDEM1", "IDEM2", "300.00")))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String secondBody = mockMvc.perform(post("/api/v1/transfers")
                .header("Authorization", "Bearer idem1")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferJson("IDEM1", "IDEM2", "300.00")))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        JsonNode first = objectMapper.readTree(firstBody);
        JsonNode second = objectMapper.readTree(secondBody);
        // Replay returns the SAME ledger transaction id ...
        assertEquals(first.get("ledgerTransactionId"), second.get("ledgerTransactionId"));
        // ... and the money moved exactly once.
        assertEquals(0, new BigDecimal("4700.00").compareTo(
            ledgerRepository.getBalance(from.getId(), CURRENCY)));
    }

    // --- helpers -------------------------------------------------------------

    private UserEntity createUser(String username) {
        return userRepository.save(new UserEntity(
            UUID.randomUUID(), username + "@vaultcore.test", username, "hash", true, "USER"));
    }

    private Account createOwnedAccount(String number, UserEntity owner) {
        return accountRepository.save(new Account(number, CURRENCY, owner));
    }

    private void fund(Account account, String amount) {
        Account clearing = accountRepository.save(
            new Account("CLR-" + account.getAccountNumber(), CURRENCY));
        UUID txnId = UUID.randomUUID();
        BigDecimal value = new BigDecimal(amount);
        ledgerService.recordTransaction(txnId, List.of(
            new LedgerEntry(txnId, clearing.getId(), LedgerEntry.EntryType.DEBIT, value, CURRENCY, "seed debit"),
            new LedgerEntry(txnId, account.getId(), LedgerEntry.EntryType.CREDIT, value, CURRENCY, "seed credit")
        ));
    }

    private String transferJson(String from, String to, String amount) {
        return """
            {"fromAccount":"%s","toAccount":"%s","amount":%s,"currency":"USD"}
            """.formatted(from, to, amount);
    }
}

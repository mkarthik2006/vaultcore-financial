package com.vaultcore.core.fraud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultcore.config.IntegrationTestBase;
import com.vaultcore.core.account.Account;
import com.vaultcore.core.account.AccountRepository;
import com.vaultcore.core.ledger.LedgerEntry;
import com.vaultcore.core.ledger.LedgerService;
import com.vaultcore.security.TestSecurityConfig;
import com.vaultcore.user.UserEntity;
import com.vaultcore.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end fraud 2FA flow: a transfer at/above the threshold is blocked with a challenge, the
 * challenge is verified, and the resubmitted transfer (carrying the challenge reference) succeeds.
 * This proves the challenge is a real second factor with a completion path, not a permanent block.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class FraudChallengeFlowIT extends IntegrationTestBase {

    private static final String CURRENCY = "USD";
    private static final Pattern CODE = Pattern.compile("mock code=(\\d{6})");

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private LedgerService ledgerService;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void largeTransferRequiresChallengeThenSucceedsAfterVerification(CapturedOutput output) throws Exception {
        UserEntity payer = createUser("payer");
        UserEntity payee = createUser("payee");
        Account from = createOwnedAccount("PAYER1", payer);
        createOwnedAccount("PAYEE1", payee);
        fund(from, "50000.00");

        String body = transferJson("PAYER1", "PAYEE1", "11000.00");

        // 1) First attempt is blocked with a fraud challenge (>= 10,000 threshold).
        String challengeBody = mockMvc.perform(post("/api/v1/transfers")
                .header("Authorization", "Bearer payer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("fraud_challenge_required"))
            .andReturn().getResponse().getContentAsString();

        JsonNode challenge = objectMapper.readTree(challengeBody);
        String challengeId = challenge.get("challengeId").asText();
        assertNotNull(challengeId);

        // The mock channel delivered a 6-digit code; recover it from the captured log output.
        Matcher matcher = CODE.matcher(output.getOut());
        assertTrue(matcher.find(), "expected a delivered mock challenge code");
        String code = matcher.group(1);

        // 2) Verify the challenge.
        mockMvc.perform(post("/api/v1/fraud/challenges/" + challengeId + "/verify")
                .header("Authorization", "Bearer payer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("VERIFIED"));

        // 3) Resubmit the transfer referencing the verified challenge -> it now completes.
        mockMvc.perform(post("/api/v1/transfers")
                .header("Authorization", "Bearer payer")
                .header("X-Fraud-Challenge-Id", challengeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());
    }

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

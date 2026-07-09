package com.vaultcore.core.fraud;

import com.vaultcore.core.transfer.TransferRequestDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests (no Spring context, no database) for the fraud threshold logic and its
 * verified-challenge bypass. Runnable without Docker.
 */
@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    private static final BigDecimal THRESHOLD = new BigDecimal("10000");
    private static final String CHANNEL = "mock-sms";

    @Mock
    private FraudChallengeService challengeService;

    private FraudDetectionService service() {
        return new FraudDetectionService(THRESHOLD, true, CHANNEL, challengeService);
    }

    private TransferRequestDTO request(String amount) {
        return new TransferRequestDTO("A1", "A2", new BigDecimal(amount), "USD");
    }

    private FraudChallenge stubChallenge() {
        return new FraudChallenge("A1", new BigDecimal("11000"), "USD", "hash",
            CHANNEL, Instant.now().plusSeconds(300));
    }

    @Test
    void belowThresholdIsAllowedWithoutAnyChallenge() {
        assertDoesNotThrow(() -> service().assertTransferAllowed(request("9999.99"), null));
        verifyNoInteractions(challengeService);
    }

    @Test
    void atOrAboveThresholdWithoutChallengeIssuesOneAndBlocks() {
        when(challengeService.issue(any(), eq(CHANNEL))).thenReturn(stubChallenge());

        assertThrows(FraudChallengeRequiredException.class,
            () -> service().assertTransferAllowed(request("11000"), null));

        verify(challengeService).issue(any(), eq(CHANNEL));
    }

    @Test
    void atOrAboveThresholdWithVerifiedChallengeIsAllowed() {
        UUID id = UUID.randomUUID();
        when(challengeService.consumeIfVerified(eq(id), any())).thenReturn(true);

        assertDoesNotThrow(() -> service().assertTransferAllowed(request("11000"), id.toString()));

        verify(challengeService).consumeIfVerified(eq(id), any());
        verify(challengeService, never()).issue(any(), any());
    }

    @Test
    void atOrAboveThresholdWithUnusableChallengeIssuesFreshOneAndBlocks() {
        UUID id = UUID.randomUUID();
        when(challengeService.consumeIfVerified(eq(id), any())).thenReturn(false);
        when(challengeService.issue(any(), eq(CHANNEL))).thenReturn(stubChallenge());

        assertThrows(FraudChallengeRequiredException.class,
            () -> service().assertTransferAllowed(request("11000"), id.toString()));

        verify(challengeService).issue(any(), eq(CHANNEL));
    }

    @Test
    void disabledChallengeNeverBlocks() {
        FraudDetectionService disabled =
            new FraudDetectionService(THRESHOLD, false, CHANNEL, challengeService);
        assertDoesNotThrow(() -> disabled.assertTransferAllowed(request("999999"), null));
        verifyNoInteractions(challengeService);
    }
}

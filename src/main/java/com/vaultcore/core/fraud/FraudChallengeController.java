package com.vaultcore.core.fraud;

import com.vaultcore.audit.AuditEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Customer-facing endpoint to satisfy a 2FA fraud challenge. After a successful verification the
 * customer resubmits the original transfer with the {@code X-Fraud-Challenge-Id} header set to the
 * verified challenge id, and the transfer proceeds.
 */
@RestController
@RequestMapping("/api/v1/fraud/challenges")
public class FraudChallengeController {

    private final FraudChallengeService challengeService;
    private final AuditEventService auditEventService;

    public FraudChallengeController(FraudChallengeService challengeService,
                                    AuditEventService auditEventService) {
        this.challengeService = challengeService;
        this.auditEventService = auditEventService;
    }

    @PostMapping("/{challengeId}/verify")
    public ResponseEntity<?> verify(@PathVariable UUID challengeId,
                                    @Valid @RequestBody VerifyChallengeRequest request) {
        challengeService.verify(challengeId, request.code());
        auditEventService.record("FRAUD_CHALLENGE_VERIFIED", null, "challengeId=" + challengeId);
        return ResponseEntity.ok(Map.of(
            "challengeId", challengeId.toString(),
            "status", "VERIFIED",
            "message", "Challenge verified. Resubmit the transfer with header X-Fraud-Challenge-Id."
        ));
    }
}

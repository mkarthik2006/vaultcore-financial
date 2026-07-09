package com.vaultcore.core.fraud;

import com.vaultcore.core.transfer.TransferRequestDTO;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Fraud detection as a Spring AOP interceptor, per the specification ("Spring Interceptor or AOP").
 *
 * <p>Advises the single-argument execution primitive {@code TransferService.transfer(request)} so
 * the check runs exactly once per real transfer execution — after any idempotent-replay
 * short-circuit (which never reaches this join point) and before the ledger is touched. The
 * optional verified-challenge reference is read from the current HTTP request's
 * {@code X-Fraud-Challenge-Id} header; outside an HTTP request (e.g. internal/batch callers) it is
 * simply absent.</p>
 */
@Aspect
@Component
public class FraudDetectionAspect {

    private static final String CHALLENGE_HEADER = "X-Fraud-Challenge-Id";

    private final FraudDetectionService fraudDetectionService;

    public FraudDetectionAspect(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @Before("execution(* com.vaultcore.core.transfer.TransferService.transfer(..)) && args(request)")
    public void guard(TransferRequestDTO request) {
        fraudDetectionService.assertTransferAllowed(request, currentChallengeId());
    }

    private String currentChallengeId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getHeader(CHALLENGE_HEADER);
        }
        return null;
    }
}

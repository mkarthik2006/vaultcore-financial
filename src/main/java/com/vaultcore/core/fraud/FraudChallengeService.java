package com.vaultcore.core.fraud;

import com.vaultcore.core.transfer.TransferRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Lifecycle of fraud 2FA challenges: issue → verify → consume, with expiry.
 *
 * <p>Codes are never stored in the clear — only a BCrypt hash is persisted, and the plaintext is
 * handed to the (mock) delivery channel exactly once.</p>
 */
@Service
public class FraudChallengeService {

    private final FraudChallengeRepository repository;
    private final FraudNotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    public FraudChallengeService(FraudChallengeRepository repository,
                                 FraudNotificationService notificationService,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${app.fraud.challenge-ttl-seconds:300}") long ttlSeconds) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    /** Issues a new PENDING challenge for the given transfer intent and delivers the code (mock). */
    @Transactional
    public FraudChallenge issue(TransferRequestDTO request, String channel) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        FraudChallenge challenge = new FraudChallenge(
            request.fromAccount(),
            request.amount(),
            request.currency().toUpperCase(),
            passwordEncoder.encode(code),
            channel,
            Instant.now().plus(ttl));
        FraudChallenge saved = repository.save(challenge);
        notificationService.sendChallenge(request.fromAccount(), channel, code);
        return saved;
    }

    /**
     * Verifies a customer-supplied code, moving PENDING → VERIFIED.
     *
     * @throws IllegalArgumentException if the challenge is unknown, expired, already used, or the
     *                                  code is wrong
     */
    @Transactional
    public void verify(UUID challengeId, String code) {
        FraudChallenge challenge = repository.findById(challengeId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown challenge"));

        if (challenge.isExpired(Instant.now())) {
            challenge.markExpired();
            throw new IllegalArgumentException("Challenge has expired");
        }
        if (challenge.getStatus() != FraudChallenge.Status.PENDING) {
            throw new IllegalArgumentException("Challenge is not awaiting verification");
        }
        if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
            throw new IllegalArgumentException("Invalid challenge code");
        }
        challenge.markVerified();
    }

    /**
     * Consumes a VERIFIED challenge if it matches the transfer intent and has not expired.
     *
     * @return {@code true} if the transfer may proceed; {@code false} otherwise (caller should issue
     *         a fresh challenge)
     */
    @Transactional
    public boolean consumeIfVerified(UUID challengeId, TransferRequestDTO request) {
        FraudChallenge challenge = repository.findById(challengeId).orElse(null);
        if (challenge == null) {
            return false;
        }
        if (challenge.isExpired(Instant.now())) {
            challenge.markExpired();
            return false;
        }
        if (challenge.getStatus() != FraudChallenge.Status.VERIFIED || !challenge.matches(request)) {
            return false;
        }
        challenge.markConsumed();
        return true;
    }
}

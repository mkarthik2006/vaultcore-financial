package com.vaultcore.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    public RefreshTokenService(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to hash token", ex);
        }
    }

    @Transactional
    public RefreshToken rotateToken(UserPrincipal user, String rawToken, Instant newExpiry) {
        String hash = hashToken(rawToken);
        RefreshToken existing = repository.findByTokenHashForUpdate(hash)
            .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (existing.isRevoked()) {
            throw new IllegalArgumentException("Refresh token revoked");
        }
        if (existing.getExpiry().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }

        existing.revoke();
        repository.save(existing);

        RefreshToken replacement = new RefreshToken(user.getId(), hashToken(rawToken), newExpiry);
        return repository.save(replacement);
    }

    public RefreshToken storeToken(UserPrincipal user, String rawToken, Instant expiry) {
        RefreshToken token = new RefreshToken(user.getId(), hashToken(rawToken), expiry);
        return repository.save(token);
    }
}
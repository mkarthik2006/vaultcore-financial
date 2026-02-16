package com.vaultcore.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthTokens login(String username, String password) {
        var auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password)
        );
        UserPrincipal user = (UserPrincipal) auth.getPrincipal();

        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);

        refreshTokenService.storeToken(user, refresh, Instant.now().plus(Duration.ofDays(7)));

        return new AuthTokens(access, refresh);
    }

    @Transactional
    public AuthTokens refresh(UserPrincipal user, String rawRefresh) {
        jwtService.assertRefreshToken(rawRefresh);
        refreshTokenService.rotateToken(user, rawRefresh, Instant.now().plus(Duration.ofDays(7)));

        String newAccess = jwtService.generateAccessToken(user);
        String newRefresh = jwtService.generateRefreshToken(user);

        refreshTokenService.storeToken(user, newRefresh, Instant.now().plus(Duration.ofDays(7)));

        return new AuthTokens(newAccess, newRefresh);
    }

    public record AuthTokens(String accessToken, String refreshToken) {}
}
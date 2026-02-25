package com.vaultcore.security;

import org.springframework.stereotype.Service;

@Service
public class AuthService {
    public record AuthTokens(String accessToken, String refreshToken) {}
}
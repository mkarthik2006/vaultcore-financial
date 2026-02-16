package com.vaultcore.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final Key signingKey;
    private final long accessTtlMillis;
    private final long refreshTtlMillis;
    private final String issuer;
    private final String audience;
    private final long clockSkewSeconds;

    public JwtService(
        @Value("${JWT_SECRET}") String jwtSecret,
        @Value("${security.jwt.access-ttl-minutes:15}") long accessTtlMinutes,
        @Value("${security.jwt.refresh-ttl-days:7}") long refreshTtlDays,
        @Value("${security.jwt.issuer:vaultcore}") String issuer,
        @Value("${security.jwt.audience:vaultcore-users}") String audience,
        @Value("${security.jwt.clock-skew-seconds:30}") long clockSkewSeconds
    ) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMillis = accessTtlMinutes * 60 * 1000;
        this.refreshTtlMillis = refreshTtlDays * 24 * 60 * 60 * 1000;
        this.issuer = issuer;
        this.audience = audience;
        this.clockSkewSeconds = clockSkewSeconds;
    }

    public String generateAccessToken(UserPrincipal user) {
        return generateToken(user.getUsername(), "access", accessTtlMillis);
    }

    public String generateRefreshToken(UserPrincipal user) {
        return generateToken(user.getUsername(), "refresh", refreshTtlMillis);
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .setAllowedClockSkewSeconds(clockSkewSeconds)
            .requireIssuer(issuer)
            .requireAudience(audience)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public void assertRefreshToken(String token) {
        Claims claims = parseClaims(token);
        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new IllegalArgumentException("Invalid token type");
        }
    }

    public boolean isTokenValid(String token, UserPrincipal user) {
        Claims claims = parseClaims(token);
        return user.getUsername().equals(claims.getSubject());
    }

    private String generateToken(String subject, String type, long ttlMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMillis);

        return Jwts.builder()
            .setSubject(subject)
            .setIssuer(issuer)
            .setAudience(audience)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .claim("type", type)
            .claim("jti", UUID.randomUUID().toString())
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }
}
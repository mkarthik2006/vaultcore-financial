package com.vaultcore.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthController(AuthService authService,
                          CustomUserDetailsService userDetailsService,
                          JwtService jwtService) {
        this.authService = authService;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        var tokens = authService.login(request.username(), request.password());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
            .httpOnly(true)
            .secure(true)
            .path("/auth/refresh")
            .sameSite("Strict")
            .maxAge(Duration.ofDays(7))
            .build();

        return ResponseEntity.ok()
            .header("Set-Cookie", cookie.toString())
            .body(Map.of(
                "tokenType", "Bearer",
                "accessToken", tokens.accessToken(),
                "accessTtlMinutes", Duration.ofMinutes(15).toMinutes()
            ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        String rawRefresh = extractRefreshToken(request);
        String username = jwtService.extractUsername(rawRefresh);

        UserPrincipal user = (UserPrincipal) userDetailsService.loadUserByUsername(username);
        var tokens = authService.refresh(user, rawRefresh);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
            .httpOnly(true)
            .secure(true)
            .path("/auth/refresh")
            .sameSite("Strict")
            .maxAge(Duration.ofDays(7))
            .build();

        return ResponseEntity.ok()
            .header("Set-Cookie", cookie.toString())
            .body(Map.of(
                "tokenType", "Bearer",
                "accessToken", tokens.accessToken(),
                "accessTtlMinutes", Duration.ofMinutes(15).toMinutes()
            ));
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new IllegalArgumentException("Refresh token cookie missing");
        }
        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        throw new IllegalArgumentException("Refresh token cookie missing");
    }

    public record AuthRequest(String username, String password) {}
}
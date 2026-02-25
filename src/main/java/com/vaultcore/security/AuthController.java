package com.vaultcore.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.status(501).body(Map.of(
            "error", "not_implemented",
            "message", "Username/password login is disabled under OAuth2/OIDC."
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh() {
        return ResponseEntity.status(501).body(Map.of(
            "error", "not_implemented",
            "message", "Refresh token flow is disabled under OAuth2/OIDC."
        ));
    }

    public record AuthRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Size(min = 8, max = 100) String password
    ) {}
}
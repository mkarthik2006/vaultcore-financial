package com.vaultcore.security;

import com.vaultcore.user.UserEntity;
import com.vaultcore.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Resolves the currently authenticated principal (a Keycloak-issued JWT) to the local
 * {@link UserEntity} that backs account ownership.
 *
 * <p>Used by the application boundary (controllers) to enforce object-level authorization: a caller
 * may only act on accounts they own. Extracting identity here — rather than trusting an account
 * number supplied in the request body — is what prevents the IDOR flagged in the audit.</p>
 */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @return the authenticated user's local record
     * @throws AccessDeniedException if the request is unauthenticated or the principal has no local
     *                               user record (never leaks which case occurred)
     */
    public UserEntity requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        String username = extractUsername(authentication);
        if (username == null || username.isBlank()) {
            throw new AccessDeniedException("Authenticated principal has no username claim");
        }
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new AccessDeniedException("Principal is not a provisioned user"));
    }

    private String extractUsername(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            return (preferredUsername != null && !preferredUsername.isBlank())
                ? preferredUsername
                : jwt.getSubject();
        }
        return authentication.getName();
    }
}

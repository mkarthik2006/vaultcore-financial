package com.vaultcore.core.admin;

import com.vaultcore.core.account.Account;
import com.vaultcore.core.account.AccountRepository;
import com.vaultcore.user.UserEntity;
import com.vaultcore.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProvisioningController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminProvisioningController(UserRepository userRepository,
                                       AccountRepository accountRepository,
                                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("User already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        String roles = (request.roles() == null || request.roles().isBlank())
            ? "USER"
            : request.roles().trim();

        boolean enabled = request.enabled() == null || request.enabled();

        // Authentication is delegated to Keycloak, but any locally-stored credential must still be
        // a strong one-way hash. A provided value is treated as a raw secret and BCrypt-hashed; when
        // absent we store a BCrypt hash of a random value so the column never holds a usable/known
        // credential (replaces the previous plaintext "external" placeholder).
        String rawPassword = request.passwordHash();
        String passwordHash = (rawPassword == null || rawPassword.isBlank())
            ? passwordEncoder.encode(UUID.randomUUID().toString())
            : passwordEncoder.encode(rawPassword.trim());

        UserEntity user = new UserEntity(
            UUID.randomUUID(),
            email,
            username,
            passwordHash,
            enabled,
            roles
        );

        UserEntity saved = userRepository.save(user);

        URI location = URI.create("/api/v1/admin/users/" + saved.getId());
        return ResponseEntity.created(location).body(UserResponse.from(saved));
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        String accountNumber = request.accountNumber().trim();
        String currency = request.currency().trim().toUpperCase();

        if (currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be 3 letters");
        }

        if (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
            throw new IllegalArgumentException("Account already exists: " + accountNumber);
        }

        // Optionally bind the account to an owning user so object-level authorization can be
        // enforced on transfers/balances. Backwards compatible: omitting ownerUsername creates an
        // unowned (e.g. system/clearing) account exactly as before.
        Account account;
        if (request.ownerUsername() != null && !request.ownerUsername().isBlank()) {
            UserEntity owner = userRepository.findByUsername(request.ownerUsername().trim())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Owner user not found: " + request.ownerUsername()));
            account = accountRepository.save(new Account(accountNumber, currency, owner));
        } else {
            account = accountRepository.save(new Account(accountNumber, currency));
        }

        URI location = URI.create("/api/v1/admin/accounts/" + account.getId());
        return ResponseEntity.created(location).body(new AccountResponse(
            account.getId(),
            account.getAccountNumber(),
            account.getCurrency()
        ));
    }
}
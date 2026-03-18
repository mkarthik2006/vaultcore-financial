package com.vaultcore.core.admin;

import com.vaultcore.core.account.Account;
import com.vaultcore.core.account.AccountRepository;
import com.vaultcore.user.UserEntity;
import com.vaultcore.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminProvisioningController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public AdminProvisioningController(UserRepository userRepository,
                                       AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
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

        String passwordHash = (request.passwordHash() == null || request.passwordHash().isBlank())
            ? "external"
            : request.passwordHash().trim();

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

        Account account = accountRepository.save(new Account(accountNumber, currency));

        URI location = URI.create("/api/v1/admin/accounts/" + account.getId());
        return ResponseEntity.created(location).body(new AccountResponse(
            account.getId(),
            account.getAccountNumber(),
            account.getCurrency()
        ));
    }
}
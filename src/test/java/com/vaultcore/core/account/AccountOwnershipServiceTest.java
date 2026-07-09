package com.vaultcore.core.account;

import com.vaultcore.user.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for object-level authorization. Runnable without Docker.
 */
@ExtendWith(MockitoExtension.class)
class AccountOwnershipServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private UserEntity caller() {
        return new UserEntity(UUID.randomUUID(), "u@vaultcore.test", "u", "hash", true, "USER");
    }

    @Test
    void returnsAccountWhenCallerOwnsIt() {
        UserEntity caller = caller();
        Account owned = new Account("A1", "USD", caller);
        when(accountRepository.findByAccountNumberAndOwner_Id(eq("A1"), eq(caller.getId())))
            .thenReturn(Optional.of(owned));

        Account result = new AccountOwnershipService(accountRepository)
            .requireOwnedAccount(caller, "A1");

        assertSame(owned, result);
    }

    @Test
    void deniesWhenCallerDoesNotOwnAccount() {
        UserEntity caller = caller();
        when(accountRepository.findByAccountNumberAndOwner_Id(eq("A1"), eq(caller.getId())))
            .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
            () -> new AccountOwnershipService(accountRepository).requireOwnedAccount(caller, "A1"));
    }
}

package com.vaultcore.core.account;

import com.vaultcore.user.UserEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Object-level authorization for accounts: verifies that a given user owns a given account.
 *
 * <p>Resolution is performed with an owner-scoped query ({@code findByAccountNumberAndOwner_Id}) so
 * that a non-owner cannot distinguish "account does not exist" from "account exists but you do not
 * own it" — both yield {@link AccessDeniedException}. This closes both the IDOR and the
 * account-enumeration findings from the audit.</p>
 */
@Service
public class AccountOwnershipService {

    private final AccountRepository accountRepository;

    public AccountOwnershipService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * @return the owned {@link Account}
     * @throws AccessDeniedException if {@code caller} does not own {@code accountNumber} (or it does
     *                               not exist)
     */
    public Account requireOwnedAccount(UserEntity caller, String accountNumber) {
        return accountRepository.findByAccountNumberAndOwner_Id(accountNumber, caller.getId())
            .orElseThrow(() -> new AccessDeniedException("Access to the requested account is denied"));
    }
}

package com.vaultcore.core.ledger;

import com.vaultcore.core.account.AccountOwnershipService;
import com.vaultcore.security.CurrentUserProvider;
import com.vaultcore.user.UserEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final LedgerQueryService ledgerQueryService;
    private final CurrentUserProvider currentUserProvider;
    private final AccountOwnershipService accountOwnershipService;

    public LedgerController(LedgerQueryService ledgerQueryService,
                           CurrentUserProvider currentUserProvider,
                           AccountOwnershipService accountOwnershipService) {
        this.ledgerQueryService = ledgerQueryService;
        this.currentUserProvider = currentUserProvider;
        this.accountOwnershipService = accountOwnershipService;
    }

    @GetMapping("/balance")
    public BigDecimal getBalance(@RequestParam String accountNumber,
                                 @RequestParam String currency) {
        // Object-level authorization: the caller may only read a balance for an account they own.
        UserEntity caller = currentUserProvider.requireCurrentUser();
        accountOwnershipService.requireOwnedAccount(caller, accountNumber);

        return ledgerQueryService.getBalance(accountNumber, currency);
    }
}
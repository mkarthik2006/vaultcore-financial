package com.vaultcore.core.transfer;

import com.vaultcore.core.account.AccountOwnershipService;
import com.vaultcore.security.CurrentUserProvider;
import com.vaultcore.user.UserEntity;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;
    private final CurrentUserProvider currentUserProvider;
    private final AccountOwnershipService accountOwnershipService;

    public TransferController(TransferService transferService,
                             CurrentUserProvider currentUserProvider,
                             AccountOwnershipService accountOwnershipService) {
        this.transferService = transferService;
        this.currentUserProvider = currentUserProvider;
        this.accountOwnershipService = accountOwnershipService;
    }

    @PostMapping
    public ResponseEntity<TransferResponseDTO> transfer(
            @Valid @RequestBody TransferRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // Object-level authorization: the caller may only debit an account they own.
        UserEntity caller = currentUserProvider.requireCurrentUser();
        accountOwnershipService.requireOwnedAccount(caller, request.fromAccount());

        TransferResponseDTO response = transferService.transfer(request, idempotencyKey);

        URI location = URI.create("/api/v1/transfers/" + response.ledgerTransactionId());
        return ResponseEntity.created(location).body(response);
    }
}
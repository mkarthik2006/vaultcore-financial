package com.vaultcore.core.transfer;

import com.vaultcore.audit.AuditEventService;
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
    private final AuditEventService auditEventService;

    public TransferController(TransferService transferService,
                             CurrentUserProvider currentUserProvider,
                             AccountOwnershipService accountOwnershipService,
                             AuditEventService auditEventService) {
        this.transferService = transferService;
        this.currentUserProvider = currentUserProvider;
        this.accountOwnershipService = accountOwnershipService;
        this.auditEventService = auditEventService;
    }

    @PostMapping
    public ResponseEntity<TransferResponseDTO> transfer(
            @Valid @RequestBody TransferRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // Object-level authorization: the caller may only debit an account they own.
        UserEntity caller = currentUserProvider.requireCurrentUser();
        accountOwnershipService.requireOwnedAccount(caller, request.fromAccount());

        TransferResponseDTO response = transferService.transfer(request, idempotencyKey);

        auditEventService.record("TRANSFER_EXECUTED", caller.getUsername(),
            "from=" + request.fromAccount() + " to=" + request.toAccount()
                + " amount=" + request.amount() + " " + request.currency()
                + " ledgerTxn=" + response.ledgerTransactionId());

        URI location = URI.create("/api/v1/transfers/" + response.ledgerTransactionId());
        return ResponseEntity.created(location).body(response);
    }
}
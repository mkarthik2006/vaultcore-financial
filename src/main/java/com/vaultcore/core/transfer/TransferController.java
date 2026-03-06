package com.vaultcore.core.transfer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponseDTO> transfer(@RequestBody TransferRequestDTO request) {
        TransferResponseDTO response = transferService.transfer(request);

        // We don’t yet have a GET endpoint for the resource, but 201 is required by you.
        // So we return a best-effort Location that points to the transfer collection + ledgerTransactionId.
        URI location = URI.create("/api/v1/transfers/" + response.ledgerTransactionId());

        return ResponseEntity.created(location).body(response);
    }
}
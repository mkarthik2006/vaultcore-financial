package com.vaultcore.core.transfer;

import jakarta.validation.Valid;
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
    public ResponseEntity<TransferResponseDTO> transfer(@Valid @RequestBody TransferRequestDTO request) {
        TransferResponseDTO response = transferService.transfer(request);

        URI location = URI.create("/api/v1/transfers/" + response.ledgerTransactionId());
        return ResponseEntity.created(location).body(response);
    }
}
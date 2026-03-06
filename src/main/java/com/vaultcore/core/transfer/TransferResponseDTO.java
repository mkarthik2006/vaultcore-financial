package com.vaultcore.core.transfer;

import java.util.UUID;

public record TransferResponseDTO(
    UUID transactionReferenceId,
    UUID ledgerTransactionId
) {
}
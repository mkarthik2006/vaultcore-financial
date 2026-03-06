package com.vaultcore.core.transfer;

import java.math.BigDecimal;

public record TransferRequestDTO(
    String fromAccount,
    String toAccount,
    BigDecimal amount,
    String currency
) {
}
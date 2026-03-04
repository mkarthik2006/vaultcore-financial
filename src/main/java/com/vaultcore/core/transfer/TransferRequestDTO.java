package com.vaultcore.core.transfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequestDTO(
    @NotBlank String fromAccount,
    @NotBlank String toAccount,
    @NotNull @Positive BigDecimal amount,
    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be 3 letters (e.g., USD)")
    String currency
) {
}
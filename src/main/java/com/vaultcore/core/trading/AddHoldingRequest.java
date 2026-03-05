package com.vaultcore.core.trading;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AddHoldingRequest(
    @NotBlank
    @Size(min = 1, max = 10)
    @Pattern(regexp = "^[A-Za-z0-9.]+$", message = "symbol must be alphanumeric (dots allowed)")
    String symbol,

    @NotNull @Positive
    BigDecimal quantity,

    @NotNull @Positive
    BigDecimal price
) {
}
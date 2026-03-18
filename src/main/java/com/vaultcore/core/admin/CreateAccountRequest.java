package com.vaultcore.core.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
    @NotBlank String accountNumber,
    @NotBlank @Size(min = 3, max = 3) String currency
) {}
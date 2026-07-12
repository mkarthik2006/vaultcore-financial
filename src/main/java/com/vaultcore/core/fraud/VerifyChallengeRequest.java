package com.vaultcore.core.fraud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyChallengeRequest(
    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$", message = "code must be 6 digits")
    String code
) {}

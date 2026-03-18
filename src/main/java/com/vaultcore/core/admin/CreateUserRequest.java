package com.vaultcore.core.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
    @NotBlank @Email String email,
    @NotBlank String username,
    String passwordHash,
    String roles,
    Boolean enabled
) {}
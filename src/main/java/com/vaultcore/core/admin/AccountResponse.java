package com.vaultcore.core.admin;

import java.util.UUID;

public record AccountResponse(
    UUID id,
    String accountNumber,
    String currency
) {}
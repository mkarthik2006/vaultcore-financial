package com.vaultcore.core.trading;

import java.math.BigDecimal;

public record HoldingDTO(
    String symbol,
    BigDecimal quantity,
    BigDecimal avgPrice,
    BigDecimal marketPrice,
    BigDecimal marketValue
) {
}
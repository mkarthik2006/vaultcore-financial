package com.vaultcore.core.trading;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PortfolioDTO(
    UUID portfolioId,
    String username,
    BigDecimal totalValue,
    List<HoldingDTO> holdings
) {
}
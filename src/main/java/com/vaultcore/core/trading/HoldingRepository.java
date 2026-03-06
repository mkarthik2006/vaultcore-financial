package com.vaultcore.core.trading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoldingRepository extends JpaRepository<Holding, UUID> {
    List<Holding> findByPortfolioId(UUID portfolioId);
    Optional<Holding> findByPortfolioIdAndSymbol(UUID portfolioId, String symbol);
}
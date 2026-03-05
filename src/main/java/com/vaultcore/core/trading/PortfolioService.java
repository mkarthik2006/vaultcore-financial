package com.vaultcore.core.trading;

import com.vaultcore.user.UserEntity;
import com.vaultcore.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final StockPriceClient stockPriceClient;

    public PortfolioService(UserRepository userRepository,
                            PortfolioRepository portfolioRepository,
                            HoldingRepository holdingRepository,
                            StockPriceClient stockPriceClient) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.stockPriceClient = stockPriceClient;
    }

    @Transactional(readOnly = true)
    public PortfolioDTO getPortfolioForUser(String username) {
        UserEntity user = findUser(username);
        Portfolio portfolio = portfolioRepository.findByUserId(user.getId())
            .orElseGet(() -> portfolioRepository.save(new Portfolio(user.getId())));

        return buildPortfolioDTO(user.getUsername(), portfolio);
    }

    @Transactional
    public PortfolioDTO addHolding(String username, AddHoldingRequest request) {
        UserEntity user = findUser(username);
        Portfolio portfolio = portfolioRepository.findByUserId(user.getId())
            .orElseGet(() -> portfolioRepository.save(new Portfolio(user.getId())));

        String symbol = request.symbol().trim().toUpperCase();

        holdingRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol)
            .ifPresentOrElse(
                holding -> {
                    holding.addToPosition(request.quantity(), request.price());
                    holdingRepository.save(holding);
                },
                () -> holdingRepository.save(new Holding(
                    portfolio.getId(),
                    symbol,
                    request.quantity(),
                    request.price()
                ))
            );

        return buildPortfolioDTO(user.getUsername(), portfolio);
    }

    @Transactional(readOnly = true)
    public PortfolioDTO getValuation(String username) {
        return getPortfolioForUser(username);
    }

    private PortfolioDTO buildPortfolioDTO(String username, Portfolio portfolio) {
        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolio.getId());

        Set<String> symbols = holdings.stream()
            .map(Holding::getSymbol)
            .collect(Collectors.toSet());

        Map<String, BigDecimal> prices = stockPriceClient.getPrices(symbols);

        List<HoldingDTO> holdingDTOs = holdings.stream()
            .map(holding -> {
                BigDecimal marketPrice = prices.getOrDefault(holding.getSymbol(), BigDecimal.ZERO);
                BigDecimal marketValue = marketPrice.multiply(holding.getQuantity());
                return new HoldingDTO(
                    holding.getSymbol(),
                    holding.getQuantity(),
                    holding.getAvgPrice(),
                    marketPrice,
                    marketValue
                );
            })
            .toList();

        BigDecimal totalValue = holdingDTOs.stream()
            .map(HoldingDTO::marketValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PortfolioDTO(
            portfolio.getId(),
            username,
            totalValue,
            holdingDTOs
        );
    }

    private UserEntity findUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
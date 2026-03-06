package com.vaultcore.core.trading;

import com.vaultcore.config.IntegrationTestBase;
import com.vaultcore.security.TestSecurityConfig;
import com.vaultcore.user.UserEntity;
import com.vaultcore.user.UserRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestSecurityConfig.class)
class PortfolioServiceTest extends IntegrationTestBase {

    private static HttpServer mockServer;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private UserRepository userRepository;

    @BeforeAll
    static void startMockServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockServer.createContext("/api/price", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String symbol = "UNKNOWN";
            if (query != null && query.contains("symbol=")) {
                symbol = query.split("symbol=")[1].toUpperCase();
            }
            String response = switch (symbol) {
                case "AAPL" -> "{\"symbol\":\"AAPL\",\"price\":185.32}";
                case "MSFT" -> "{\"symbol\":\"MSFT\",\"price\":402.18}";
                default -> "{\"symbol\":\"" + symbol + "\",\"price\":150.00}";
            };

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        });
        mockServer.start();
    }

    @AfterAll
    static void stopMockServer() {
        mockServer.stop(0);
    }

    @DynamicPropertySource
    static void registerStockApi(DynamicPropertyRegistry registry) {
        String baseUrl = "http://localhost:" + mockServer.getAddress().getPort() + "/api";
        registry.add("app.stock.api.base-url", () -> baseUrl);
        registry.add("app.stock.api.timeout-ms", () -> "200");
        registry.add("app.stock.api.cache-ttl-ms", () -> "2000");
    }

    @Test
    void createPortfolioForUser() {
        UserEntity user = userRepository.save(new UserEntity(
            UUID.randomUUID(), "trader1@vaultcore.com", "trader1", "hash", true, "USER"
        ));

        PortfolioDTO portfolio = portfolioService.getPortfolioForUser(user.getUsername());

        assertNotNull(portfolio.portfolioId());
        assertEquals("trader1", portfolio.username());
        assertTrue(portfolio.holdings().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(portfolio.totalValue()));
    }

    @Test
    void addHoldingAndUpdateExistingHolding() {
        UserEntity user = userRepository.save(new UserEntity(
            UUID.randomUUID(), "trader2@vaultcore.com", "trader2", "hash", true, "USER"
        ));

        portfolioService.addHolding(user.getUsername(), new AddHoldingRequest(
            "AAPL", new BigDecimal("2.0000"), new BigDecimal("180.00")
        ));

        PortfolioDTO afterFirst = portfolioService.getPortfolioForUser(user.getUsername());
        assertEquals(1, afterFirst.holdings().size());
        assertEquals("AAPL", afterFirst.holdings().get(0).symbol());

        portfolioService.addHolding(user.getUsername(), new AddHoldingRequest(
            "AAPL", new BigDecimal("1.0000"), new BigDecimal("190.00")
        ));

        PortfolioDTO afterSecond = portfolioService.getPortfolioForUser(user.getUsername());
        assertEquals(1, afterSecond.holdings().size());
        assertEquals(0, new BigDecimal("3.0000").compareTo(afterSecond.holdings().get(0).quantity()));
    }

    @Test
    void portfolioValuationCalculation() {
        UserEntity user = userRepository.save(new UserEntity(
            UUID.randomUUID(), "trader3@vaultcore.com", "trader3", "hash", true, "USER"
        ));

        portfolioService.addHolding(user.getUsername(), new AddHoldingRequest(
            "AAPL", new BigDecimal("2.0000"), new BigDecimal("180.00")
        ));
        portfolioService.addHolding(user.getUsername(), new AddHoldingRequest(
            "MSFT", new BigDecimal("1.0000"), new BigDecimal("400.00")
        ));

        PortfolioDTO valuation = portfolioService.getValuation(user.getUsername());

        BigDecimal expected =
            new BigDecimal("185.32").multiply(new BigDecimal("2.0000"))
                .add(new BigDecimal("402.18").multiply(new BigDecimal("1.0000")));

        assertEquals(0, expected.compareTo(valuation.totalValue()));
    }
}
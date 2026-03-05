package com.vaultcore.core.trading;

import com.sun.net.httpserver.HttpServer;
import com.vaultcore.config.IntegrationTestBase;
import com.vaultcore.security.TestSecurityConfig;
import com.vaultcore.user.UserEntity;
import com.vaultcore.user.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class PortfolioControllerIT extends IntegrationTestBase {

    private static HttpServer mockServer;

    @Autowired
    private TestRestTemplate restTemplate;

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
            String response = "{\"symbol\":\"" + symbol + "\",\"price\":185.32}";
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
    void authenticatedRequestReturnsPortfolio() {
        UserEntity user = userRepository.save(new UserEntity(
            UUID.randomUUID(), "controller@vaultcore.com", "controllerUser", "hash", true, "USER"
        ));

        ResponseEntity<PortfolioDTO> response = restTemplate.exchange(
            "/api/v1/portfolio",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(user.getUsername())),
            PortfolioDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().username()).isEqualTo(user.getUsername());
    }

    @Test
    void postHoldingsCreatesHolding() {
        UserEntity user = userRepository.save(new UserEntity(
            UUID.randomUUID(), "holdings@vaultcore.com", "holdingUser", "hash", true, "USER"
        ));

        AddHoldingRequest request = new AddHoldingRequest(
            "AAPL", new BigDecimal("2.0000"), new BigDecimal("180.00")
        );

        ResponseEntity<PortfolioDTO> response = restTemplate.exchange(
            "/api/v1/portfolio/holdings",
            HttpMethod.POST,
            new HttpEntity<>(request, authHeaders(user.getUsername())),
            PortfolioDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().holdings().size()).isEqualTo(1);
        assertThat(response.getBody().holdings().get(0).symbol()).isEqualTo("AAPL");
    }

    @Test
    void validationErrorsHandledByGlobalExceptionHandler() {
        UserEntity user = userRepository.save(new UserEntity(
            UUID.randomUUID(), "invalid@vaultcore.com", "invalidUser", "hash", true, "USER"
        ));

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            "/api/v1/portfolio/holdings",
            HttpMethod.POST,
            new HttpEntity<>("{}", authHeaders(user.getUsername())),
            new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("error");
        assertThat(response.getBody().get("error")).isEqualTo("validation_failed");
    }

    private HttpHeaders authHeaders(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(username);
        return headers;
    }
}
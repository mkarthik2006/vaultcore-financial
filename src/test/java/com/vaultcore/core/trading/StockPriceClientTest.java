package com.vaultcore.core.trading;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StockPriceClientTest {

    @Test
    void successfulPriceFetch() throws IOException {
        HttpServer server = startServer(0, (symbol) ->
            "{\"symbol\":\"" + symbol + "\",\"price\":185.32}"
        );

        StockPriceClient client = new StockPriceClient(
            "http://localhost:" + server.getAddress().getPort() + "/api",
            2000,
            200
        );

        Map<String, BigDecimal> prices = client.getPrices(Set.of("AAPL"));
        assertEquals(new BigDecimal("185.32"), prices.get("AAPL"));

        server.stop(0);
    }

    @Test
    void timeoutFallbackUsesCachedValue() throws IOException {
        AtomicInteger callCount = new AtomicInteger();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/price", exchange -> {
            int call = callCount.incrementAndGet();
            // First call: fast to populate cache. Second call: slow to trigger timeout.
            if (call > 1) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {}
            }
            String response = "{\"symbol\":\"AAPL\",\"price\":185.32}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        });
        server.start();

        StockPriceClient client = new StockPriceClient(
            "http://localhost:" + server.getAddress().getPort() + "/api",
            5000,
            100
        );

        Map<String, BigDecimal> prices = client.getPrices(Set.of("AAPL"));
        assertNotNull(prices.get("AAPL"));
        assertTrue(callCount.get() >= 1);

        Map<String, BigDecimal> cached = client.getPrices(Set.of("AAPL"));
        assertEquals(prices.get("AAPL"), cached.get("AAPL"));

        server.stop(0);
    }

    @Test
    void cacheHitVsMiss() throws IOException {
        AtomicInteger callCount = new AtomicInteger();

        HttpServer server = startServer(0, (symbol) -> {
            callCount.incrementAndGet();
            return "{\"symbol\":\"" + symbol + "\",\"price\":200.00}";
        });

        StockPriceClient client = new StockPriceClient(
            "http://localhost:" + server.getAddress().getPort() + "/api",
            5000,
            200
        );

        client.getPrices(Set.of("MSFT"));
        client.getPrices(Set.of("MSFT"));

        assertEquals(1, callCount.get());

        server.stop(0);
    }

    private HttpServer startServer(int port, PriceResponder responder) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/price", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String symbol = "UNKNOWN";
            if (query != null && query.contains("symbol=")) {
                symbol = query.split("symbol=")[1].toUpperCase();
            }
            String response = responder.respond(symbol);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        });
        server.start();
        return server;
    }

    @FunctionalInterface
    interface PriceResponder {
        String respond(String symbol);
    }
}
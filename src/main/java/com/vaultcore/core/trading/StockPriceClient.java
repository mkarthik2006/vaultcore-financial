package com.vaultcore.core.trading;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class StockPriceClient {

    private final RestClient restClient;
    private final Duration cacheTtl;
    private final Map<String, CachedQuote> cache = new ConcurrentHashMap<>();

    public StockPriceClient(
        @Value("${app.stock.api.base-url:http://stock-mock-api:8080/api}") String baseUrl,
        @Value("${app.stock.api.cache-ttl-ms:2000}") long cacheTtlMs,
        @Value("${app.stock.api.timeout-ms:250}") int timeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build();
        this.cacheTtl = Duration.ofMillis(cacheTtlMs);
    }

    public Map<String, BigDecimal> getPrices(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> normalized = symbols.stream()
            .filter(s -> s != null && !s.isBlank())
            .map(s -> s.trim().toUpperCase())
            .collect(Collectors.toSet());

        Map<String, BigDecimal> result = new ConcurrentHashMap<>();
        Instant now = Instant.now();

        for (String symbol : normalized) {
            CachedQuote cached = cache.get(symbol);
            if (isCacheValid(cached, now)) {
                result.put(symbol, cached.price());
                continue;
            }

            BigDecimal fetched = fetchPrice(symbol);
            if (fetched != null) {
                cache.put(symbol, new CachedQuote(fetched, now));
                result.put(symbol, fetched);
            } else if (cached != null) {
                // fallback to stale cached value to keep UI stable
                result.put(symbol, cached.price());
            }
        }

        return result;
    }

    private BigDecimal fetchPrice(String symbol) {
        try {
            StockQuote quote = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/price")
                    .queryParam("symbol", symbol)
                    .build()
                )
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(StockQuote.class);

            if (quote == null || quote.price() == null) {
                return null;
            }

            return quote.price();
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isCacheValid(CachedQuote cached, Instant now) {
        if (cached == null) return false;
        return cached.timestamp().plus(cacheTtl).isAfter(now);
    }

    private record CachedQuote(BigDecimal price, Instant timestamp) {}

    private record StockQuote(String symbol, BigDecimal price) {}
}
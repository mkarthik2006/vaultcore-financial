package com.vaultcore.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Distributed, fixed-window per-client rate limiter backed by <b>Redis counters</b> (INCR + EXPIRE),
 * as the specification calls for ("Redis … rate-limiting counters"). Because the counter lives in
 * Redis, the limit is enforced consistently across horizontally-scaled backend instances.
 *
 * <p>Fails <b>open</b>: if Redis is unavailable (or not configured, e.g. under the test profile) the
 * request is allowed rather than blocked, so a cache outage can never take down the API.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String KEY_PREFIX = "ratelimit:";
    private static final Duration WINDOW_TTL = Duration.ofSeconds(120);

    private final boolean enabled;
    private final int requestsPerMinute;
    private final ObjectProvider<StringRedisTemplate> redisProvider;

    public RateLimitFilter(@Value("${app.rate-limit.enabled:true}") boolean enabled,
                           @Value("${app.rate-limit.requests-per-minute:300}") int requestsPerMinute,
                           ObjectProvider<StringRedisTemplate> redisProvider) {
        this.enabled = enabled;
        this.requestsPerMinute = requestsPerMinute;
        this.redisProvider = redisProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!allow(clientKey(request))) {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"rate_limited\",\"message\":\"Too many requests; slow down.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** Increments the current minute's Redis counter for this client and enforces the limit. */
    private boolean allow(String clientKey) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return true; // no Redis (e.g. test profile) -> fail open
        }
        try {
            long minute = System.currentTimeMillis() / 60_000L;
            String key = KEY_PREFIX + clientKey + ':' + minute;
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, WINDOW_TTL); // first hit in this window sets the TTL
            }
            return count == null || count <= requestsPerMinute;
        } catch (RuntimeException ex) {
            log.warn("Rate limiter Redis error; failing open for this request", ex);
            return true;
        }
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

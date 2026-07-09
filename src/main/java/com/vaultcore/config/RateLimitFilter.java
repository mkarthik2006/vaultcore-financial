package com.vaultcore.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window per-client rate limiter (requests/minute), keyed by client IP. In-memory and
 * per-instance; for a horizontally-scaled deployment this would move to a shared Redis counter, but
 * it provides a working, configurable backstop today. Disabled automatically under the test profile.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final int requestsPerMinute;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.enabled:true}") boolean enabled,
                           @Value("${app.rate-limit.requests-per-minute:300}") int requestsPerMinute) {
        this.enabled = enabled;
        this.requestsPerMinute = requestsPerMinute;
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

    private boolean allow(String key) {
        long minute = System.currentTimeMillis() / 60_000L;
        Window window = windows.compute(key,
            (k, existing) -> (existing == null || existing.minute != minute) ? new Window(minute) : existing);
        return window.count.incrementAndGet() <= requestsPerMinute;
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        private final long minute;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long minute) {
            this.minute = minute;
        }
    }
}

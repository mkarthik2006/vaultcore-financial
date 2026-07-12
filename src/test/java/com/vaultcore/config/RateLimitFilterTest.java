package com.vaultcore.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Redis-backed rate limiter. Runs without Docker: Redis is mocked. Lives in the
 * same package so it can invoke the protected {@code doFilterInternal} directly.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;
    @Mock private ObjectProvider<StringRedisTemplate> provider;
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private RateLimitFilter filter(boolean enabled, int rpm) {
        return new RateLimitFilter(enabled, rpm, provider);
    }

    @Test
    void disabled_passesThroughWithoutTouchingRedis() throws Exception {
        filter(false, 300).doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(provider, never()).getIfAvailable();
        verify(response, never()).setStatus(429);
    }

    @Test
    void underLimit_allowsRequest() throws Exception {
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L); // first hit in the window

        filter(true, 2).doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(redis).expire(anyString(), any());        // TTL set on first hit
        verify(response, never()).setStatus(429);
    }

    @Test
    void overLimit_blocksWith429AndDoesNotProceed() throws Exception {
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(3L); // exceeds limit of 2
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter(true, 2).doFilterInternal(request, response, chain);

        verify(response).setStatus(429);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void redisError_failsOpen() throws Exception {
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenThrow(new RuntimeException("redis down"));

        filter(true, 2).doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response); // fail open
        verify(response, never()).setStatus(429);
    }
}

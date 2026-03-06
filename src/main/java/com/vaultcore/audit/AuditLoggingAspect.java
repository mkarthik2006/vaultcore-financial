package com.vaultcore.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.IntStream;

@Aspect
@Component
public class AuditLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggingAspect.class);

    @Around("execution(* com.vaultcore..*Controller.*(..)) || execution(* com.vaultcore..*Service.*(..))")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String method = sig.getDeclaringType().getSimpleName() + "." + sig.getName();

        String params = formatArgs(sig.getParameterNames(), pjp.getArgs());

        try {
            Object result = pjp.proceed();
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            log.info("AUDIT method={} params={} result={} durationMs={}",
                method, params, safeValue("return", result), durationMs);
            return result;
        } catch (Throwable ex) {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            log.info("AUDIT method={} params={} error={} durationMs={}",
                method, params, ex.getClass().getSimpleName(), durationMs);
            throw ex;
        }
    }

    private String formatArgs(String[] names, Object[] args) {
        if (args == null || args.length == 0) return "[]";
        return IntStream.range(0, args.length)
            .mapToObj(i -> {
                String name = (names != null && i < names.length) ? names[i] : "arg" + i;
                return name + "=" + safeValue(name, args[i]);
            })
            .reduce((a, b) -> a + ", " + b)
            .map(s -> "[" + s + "]")
            .orElse("[]");
    }

    private String safeValue(String name, Object value) {
        if (value == null) return "null";

        if (value instanceof HttpServletRequest
            || value instanceof HttpServletResponse
            || value instanceof Authentication
            || value instanceof Principal
            || value instanceof Jwt
            || value instanceof HttpHeaders) {
            return "<redacted>";
        }

        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("password") || lower.contains("token") || lower.contains("authorization")) {
            return "<redacted>";
        }

        if (value instanceof CharSequence cs) {
            String s = cs.toString();
            if (looksLikeJwt(s) || s.length() > 40) {
                return "<redacted>";
            }
            return s;
        }

        if (value instanceof byte[]) {
            return "<binary>";
        }

        return String.valueOf(value);
    }

    private boolean looksLikeJwt(String s) {
        if (!s.startsWith("eyJ")) return false;
        return Arrays.stream(s.split("\\.")).count() == 3;
    }
}
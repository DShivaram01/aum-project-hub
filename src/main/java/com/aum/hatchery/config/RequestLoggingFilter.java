package com.aum.hatchery.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// Logs one line per API request with its status and how long it took.
// Runs before Spring Security so 401/403 responses are logged too.
// With the "aws" profile the key-value pairs become JSON fields that CloudWatch can query.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Load balancer health checks would flood the logs
        return !path.startsWith("/api/") || path.equals("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        // Only reuse the caller's id if it is safe to write into the logs
        if (requestId == null || !requestId.matches("[A-Za-z0-9-]{1,64}")) {
            requestId = UUID.randomUUID().toString();
        }
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put("requestId", requestId);

        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            int status = response.getStatus();

            Level level = status >= 500 ? Level.ERROR : status >= 400 ? Level.WARN : Level.INFO;
            log.atLevel(level)
                    .addKeyValue("httpMethod", request.getMethod())
                    .addKeyValue("path", request.getRequestURI())
                    .addKeyValue("status", status)
                    .addKeyValue("durationMs", durationMs)
                    .log("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), status, durationMs);

            MDC.remove("requestId");
        }
    }
}

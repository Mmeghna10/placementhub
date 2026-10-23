package com.placementhub.backend.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every request a correlation id and writes one access-log line per request.
 *
 * - The id comes from an incoming X-Request-Id header (e.g. set by a proxy) if it looks
 *   safe, otherwise a new UUID. It is put in the logging MDC as "requestId", so every log
 *   line written while handling the request carries it, returned in the X-Request-Id
 *   response header, and included in JSON error bodies. A user reporting an error can
 *   quote it and the matching log lines are one search away.
 * - The access line records method, path, status and duration. Query strings are left
 *   out on purpose: they can contain search text or other personal data.
 *
 * Registered first in the filter chain (see ObservabilityConfig), before Spring Security,
 * so authentication and rate-limit log lines carry the id too.
 */
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("http.access");
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        // Never trust arbitrary client text in logs: anything unexpected gets a fresh id.
        String requestId = incoming != null && SAFE_ID.matcher(incoming).matches()
                ? incoming : UUID.randomUUID().toString();

        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            String path = request.getRequestURI();
            // Health probes run every few seconds; logging them would drown real traffic.
            if (!path.startsWith("/actuator/health")) {
                ACCESS_LOG.info("{} {} {} {}ms", request.getMethod(), path, response.getStatus(), durationMs);
            }
            MDC.remove(MDC_KEY);
        }
    }
}

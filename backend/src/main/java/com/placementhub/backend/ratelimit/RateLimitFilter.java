package com.placementhub.backend.ratelimit;

import com.placementhub.backend.security.SecurityErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Applies RateLimitRules inside the Spring Security chain, after JwtFilter, so per-user
 * rules can see who is logged in. Over the limit the request stops here with
 * 429 Too Many Requests, a Retry-After header and the usual JSON error body; it never
 * reaches the controller (or BCrypt, or the Gemini API).
 *
 * Not a Spring @Component on purpose: Spring Boot would also register a component
 * filter in the plain servlet chain, outside Spring Security, where the user is unknown.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final List<RateLimitRule> rules;
    private final SecurityErrorWriter errorWriter;

    public RateLimitFilter(RateLimiter rateLimiter, List<RateLimitRule> rules, SecurityErrorWriter errorWriter) {
        this.rateLimiter = rateLimiter;
        this.rules = rules;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        for (RateLimitRule rule : rules) {
            if (!rule.matches(request.getMethod(), path)) {
                continue;
            }
            Duration wait = rateLimiter.tryAcquire(rule, clientKey(rule, request));
            if (!wait.isZero()) {
                long retryAfterSeconds = Math.max(1, (wait.toMillis() + 999) / 1000);
                log.warn("Rate limit '{}' exceeded on {} {}", rule.name(), request.getMethod(), path);
                response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
                errorWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                        "Too many requests. Please wait " + retryAfterSeconds + " seconds and try again.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private String clientKey(RateLimitRule rule, HttpServletRequest request) {
        if (rule.byUser()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                return "user:" + auth.getName();
            }
        }
        return "ip:" + clientIp(request);
    }

    /**
     * The client's IP. Behind Azure App Service, Spring Boot detects the platform and
     * applies the X-Forwarded-For header, so getRemoteAddr() is the real client rather
     * than Azure's front end. Azure may append a port ("203.0.113.5:51234"); it is
     * stripped so that every new connection from the same client shares one bucket.
     */
    static String clientIp(HttpServletRequest request) {
        String address = request.getRemoteAddr();
        if (address != null && address.indexOf(':') == address.lastIndexOf(':') && address.contains(".")) {
            int colon = address.indexOf(':');
            if (colon > 0) {
                return address.substring(0, colon);
            }
        }
        return address;
    }
}

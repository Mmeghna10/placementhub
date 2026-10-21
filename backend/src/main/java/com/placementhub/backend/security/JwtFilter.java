package com.placementhub.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates requests that carry a valid "Authorization: Bearer" token.
 *
 * A bad token never throws out of the filter. The request simply stays anonymous, so public
 * endpoints still work (e.g. logging in while a stale token sits in localStorage) and protected
 * endpoints get a clean 401 from SecurityErrorWriter, which reads the reason stored here.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE = "placementhub.authError";

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtUtil.parseAndValidate(authHeader.substring(7));
            // Role and active flag come from the database, not the token, so a role change
            // or deactivation takes effect on the next request.
            UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());

            if (!userDetails.isEnabled()) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthError.ACCOUNT_DISABLED);
            } else {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (ExpiredJwtException e) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthError.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthError.INVALID_TOKEN);
        }

        filterChain.doFilter(request, response);
    }

    public enum AuthError {
        TOKEN_EXPIRED("Your session has expired. Please log in again."),
        INVALID_TOKEN("Invalid authentication token"),
        ACCOUNT_DISABLED("This account has been deactivated");

        private final String message;

        AuthError(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}

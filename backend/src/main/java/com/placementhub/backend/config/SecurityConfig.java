package com.placementhub.backend.config;

import com.placementhub.backend.ratelimit.RateLimitFilter;
import com.placementhub.backend.ratelimit.RateLimitRule;
import com.placementhub.backend.ratelimit.RateLimiter;
import com.placementhub.backend.security.JwtFilter;
import com.placementhub.backend.security.SecurityErrorWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtFilter jwtFilter;
    private final SecurityErrorWriter securityErrorWriter;

    // Comma-separated; override with APP_CORS_ALLOWED_ORIGINS. Defaults keep local dev and the
    // current Azure frontend working without any new configuration.
    @Value("${app.cors.allowed-origins:http://localhost:5173,https://agreeable-cliff-07cedfc00.1.azurestaticapps.net}")
    private List<String> allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless API authenticated by a header token, not cookies, so CSRF does not apply.
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/error").permitAll()
                // API docs describe endpoints only; every endpoint still checks its own auth.
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Health probes for Docker/Azure: only UP/DOWN, no details (show-details: never).
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(securityErrorWriter)
                .accessDeniedHandler(securityErrorWriter)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        if (rateLimitEnabled) {
            // After JwtFilter, so the per-user chat rule knows who is calling.
            http.addFilterAfter(new RateLimitFilter(rateLimiter(), rateLimitRules(), securityErrorWriter), JwtFilter.class);
        }

        return http.build();
    }

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.rate-limit.login-per-minute:10}")
    private int loginPerMinute;

    @Value("${app.rate-limit.register-per-10-minutes:5}")
    private int registerPer10Minutes;

    @Value("${app.rate-limit.chat-per-10-minutes:20}")
    private int chatPer10Minutes;

    @Bean
    public RateLimiter rateLimiter() {
        return new RateLimiter();
    }

    /**
     * login and register are limited per IP address (the caller is not logged in yet);
     * chat is limited per user, because every message costs a Gemini API call.
     */
    private List<RateLimitRule> rateLimitRules() {
        return List.of(
                new RateLimitRule("login", "POST", "/api/auth/login", loginPerMinute, Duration.ofMinutes(1), false),
                new RateLimitRule("register", "POST", "/api/auth/register", registerPer10Minutes, Duration.ofMinutes(10), false),
                new RateLimitRule("chat", "POST", "/api/chat", chatPer10Minutes, Duration.ofMinutes(10), true));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins.stream().map(String::trim).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // The frontend sends the JWT in a header and uses no cookies, so credentials are not needed.
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

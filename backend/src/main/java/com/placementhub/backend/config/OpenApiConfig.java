package com.placementhub.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API documentation served at /swagger-ui.html (JSON at /v3/api-docs).
 *
 * Every endpoint except /api/auth/** needs a JWT: log in via POST /api/auth/login, copy
 * the token, click "Authorize" and paste it (without the "Bearer " prefix).
 */
@Configuration
public class OpenApiConfig {

    static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI placementHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PlacementHub API")
                        .version("v1")
                        .description("""
                                Campus placement platform: students apply to jobs, recruiters manage \
                                applicants and interview slots, admins approve postings.

                                **Errors** always use one JSON shape: \
                                `{ "error": "<message>", "status": 409, "code": "SLOT_FULL", "path": "...", "timestamp": "..." }` \
                                plus `fieldErrors` for validation failures (400).

                                **Common codes:** 401 `UNAUTHORIZED` / `TOKEN_EXPIRED` / `INVALID_TOKEN`, \
                                403 `ACCESS_DENIED`, 404 `NOT_FOUND` (also for other users' resources), \
                                409 conflicts (`APPLICATION_ALREADY_EXISTS`, `SLOT_FULL`, `APPLICATION_MODIFIED`, \
                                `CONCURRENT_UPDATE`), 422 business rules (`DEADLINE_PASSED`, `NOT_ELIGIBLE`, \
                                `INVALID_STATUS_TRANSITION`), 429 `RATE_LIMITED`."""))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token from POST /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}

package com.placementhub.backend.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The API docs load without logging in and describe the real endpoints and auth. */
@AutoConfigureMockMvc
class OpenApiDocsIntegrationTest extends MySqlIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void openApiJsonDescribesEndpointsAndBearerAuth() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("PlacementHub API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/students/jobs/search'].get").exists())
                .andExpect(jsonPath("$.paths['/api/students/applications/{id}/slots/{slotId}/booking'].post").exists())
                // Login and register are documented as public (no lock icon).
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").isEmpty());
    }

    @Test
    void swaggerUiPageIsServed() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }

    @Test
    void docsBeingPublicDoesNotOpenTheApiItself() throws Exception {
        mockMvc.perform(get("/api/students/jobs/search")).andExpect(status().isUnauthorized());
    }
}

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
    void keyEndpointsShowExampleRequestsResponsesAndFailures() throws Exception {
        String apply = "$.paths['/api/students/jobs/{jobId}/apply'].post.responses";
        String status = "$.paths['/api/recruiter/applications/{id}/status'].put";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.requestBody.content['application/json'].examples").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['401'].content['application/json'].example.code")
                        .value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath(apply + "['201'].content['application/json'].example.status").value("APPLIED"))
                .andExpect(jsonPath(apply + "['409'].content['application/json'].examples['Already applied'].value.code")
                        .value("APPLICATION_ALREADY_EXISTS"))
                .andExpect(jsonPath(apply + "['422'].content['application/json'].examples['Not eligible']").exists())
                .andExpect(jsonPath(status + ".requestBody.content['application/json'].examples['Shortlist'].value.version")
                        .value(0))
                .andExpect(jsonPath(status + ".responses['409'].content['application/json'].examples['Stale version'].value.code")
                        .value("APPLICATION_MODIFIED"));
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

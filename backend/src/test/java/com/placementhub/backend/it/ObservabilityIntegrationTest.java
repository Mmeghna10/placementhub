package com.placementhub.backend.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Correlation ids and the admin-only metrics endpoint, end to end through the filter chain. */
@AutoConfigureMockMvc
class ObservabilityIntegrationTest extends MySqlIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void everyResponseGetsARequestIdThatAlsoAppearsInErrorBodies() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/students/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andReturn();

        String requestId = result.getResponse().getHeader("X-Request-Id");
        assertThat(requestId).hasSizeGreaterThanOrEqualTo(8);
        assertThat(result.getResponse().getContentAsString()).contains("\"requestId\":\"" + requestId + "\"");
    }

    @Test
    void aSafeIncomingRequestIdIsKeptSoProxiesAndClientsCanTraceIt() throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Request-Id", "edge-abc123-xyz"))
                .andExpect(header().string("X-Request-Id", "edge-abc123-xyz"));
    }

    @Test
    void anUnsafeIncomingRequestIdIsReplaced() throws Exception {
        // Anything that could forge or break log lines is not trusted.
        MvcResult result = mockMvc.perform(get("/actuator/health").header("X-Request-Id", "bad id\nINFO fake log line"))
                .andReturn();
        assertThat(result.getResponse().getHeader("X-Request-Id")).doesNotContain(" ", "\n");
    }

    @Test
    void metricsAreOnlyForAdmins() throws Exception {
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").with(user("s@example.com").roles("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/metrics").with(user("a@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names", hasItems("http.server.requests", "hikaricp.connections.active",
                        "jvm.memory.used")));
    }

    @Test
    void latencyPercentilesArePublished() throws Exception {
        mockMvc.perform(get("/api/students/profile")); // produce at least one timed request
        mockMvc.perform(get("/actuator/metrics/http.server.requests.percentile").with(user("a@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableTags[?(@.tag == 'phi')].values[*]", hasItems("0.5", "0.95", "0.99")));
    }
}

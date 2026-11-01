package com.aum.hatchery.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class MonitoringIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorHealthIsPublicAndIncludesDatabase() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void metricsEndpointIsNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void everyApiResponseHasARequestId() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));

        mockMvc.perform(get("/api/projects").header("X-Request-Id", "abc-123"))
                .andExpect(header().string("X-Request-Id", "abc-123"));
    }

    @Test
    void unsafeRequestIdIsReplaced() throws Exception {
        String id = mockMvc.perform(get("/api/projects").header("X-Request-Id", "bad\nid"))
                .andReturn().getResponse().getHeader("X-Request-Id");

        assertThat(id).doesNotContain("\n").isNotEqualTo("bad\nid");
    }

    @Test
    void requestsAreLoggedWithStatusAndDuration(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());

        assertThat(output).containsPattern("GET /api/users/me -> 401 \\(\\d+ ms\\)");
    }
}

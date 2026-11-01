package com.aum.projecthub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ValidationApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void unknownStatusFilterReturns400() throws Exception {
        mockMvc.perform(get("/api/projects").param("status", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for 'status'"));
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/projects/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void unknownRoleReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "A", "email": "a@aum.edu", "password": "password123", "role": "ADMIN"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordLongerThan72CharactersReturns400() throws Exception {
        String longPassword = "a".repeat(73);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "A", "email": "a@aum.edu", "password": "%s", "role": "STUDENT"}
                                """.formatted(longPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void tooLongDepartmentReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "A", "email": "a@aum.edu", "password": "password123",
                                 "role": "STUDENT", "department": "%s"}
                                """.formatted("x".repeat(300))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.department").exists());
    }

    @Test
    @WithMockUser
    void unknownApiPathReturns404NotServerError() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void demoEndpointIsOffByDefault() throws Exception {
        mockMvc.perform(get("/api/demo"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void wrongHttpMethodReturns405() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed());
    }
}

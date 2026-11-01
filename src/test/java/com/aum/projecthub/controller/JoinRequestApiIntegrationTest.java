package com.aum.projecthub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JoinRequestApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullJoinRequestFlow() throws Exception {
        String ownerToken = registerAndLogin("owner@aum.edu");
        String studentToken = registerAndLogin("student@aum.edu");
        long projectId = createProject(ownerToken);

        MvcResult created = mockMvc.perform(post("/api/projects/" + projectId + "/join-requests")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"I know Java\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.projectTitle").value("Campus Map"))
                .andReturn();
        long requestId = readJson(created).get("id").asLong();

        mockMvc.perform(get("/api/join-requests/mine").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/projects/" + projectId + "/join-requests")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("I know Java"));

        mockMvc.perform(put("/api/join-requests/" + requestId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(put("/api/join-requests/" + requestId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"REJECTED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void joinRequestNeedsLogin() throws Exception {
        String ownerToken = registerAndLogin("owner@aum.edu");
        long projectId = createProject(ownerToken);

        mockMvc.perform(post("/api/projects/" + projectId + "/join-requests"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/projects/" + projectId + "/join-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRequestReturns409() throws Exception {
        String ownerToken = registerAndLogin("owner@aum.edu");
        String studentToken = registerAndLogin("student@aum.edu");
        long projectId = createProject(ownerToken);

        sendJoinRequest(projectId, studentToken);

        mockMvc.perform(post("/api/projects/" + projectId + "/join-requests")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isConflict());
    }

    @Test
    void ownerCannotJoinOwnProject() throws Exception {
        String ownerToken = registerAndLogin("owner@aum.edu");
        long projectId = createProject(ownerToken);

        mockMvc.perform(post("/api/projects/" + projectId + "/join-requests")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyOwnerCanSeeAndDecideRequests() throws Exception {
        String ownerToken = registerAndLogin("owner@aum.edu");
        String studentToken = registerAndLogin("student@aum.edu");
        String otherToken = registerAndLogin("other@aum.edu");
        long projectId = createProject(ownerToken);
        long requestId = sendJoinRequest(projectId, studentToken);

        mockMvc.perform(get("/api/projects/" + projectId + "/join-requests")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/join-requests/" + requestId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACCEPTED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidDecisionReturns400() throws Exception {
        String ownerToken = registerAndLogin("owner@aum.edu");
        String studentToken = registerAndLogin("student@aum.edu");
        long projectId = createProject(ownerToken);
        long requestId = sendJoinRequest(projectId, studentToken);

        mockMvc.perform(put("/api/join-requests/" + requestId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists());

        mockMvc.perform(put("/api/join-requests/" + requestId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"PENDING\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requesterCanWithdrawPendingRequest() throws Exception {
        String ownerToken = registerAndLogin("owner@aum.edu");
        String studentToken = registerAndLogin("student@aum.edu");
        long projectId = createProject(ownerToken);
        long requestId = sendJoinRequest(projectId, studentToken);

        mockMvc.perform(delete("/api/join-requests/" + requestId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/join-requests/" + requestId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/join-requests/" + requestId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingProjectRemovesItsRequests() throws Exception {
        String ownerToken = registerAndLogin("owner@aum.edu");
        String studentToken = registerAndLogin("student@aum.edu");
        long projectId = createProject(ownerToken);
        sendJoinRequest(projectId, studentToken);

        mockMvc.perform(delete("/api/projects/" + projectId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/join-requests/mine").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private long sendJoinRequest(long projectId, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects/" + projectId + "/join-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"Please add me\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asLong();
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Test User", "email": "%s", "password": "password123",
                                 "role": "STUDENT", "department": "Computer Science"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"password123\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).get("token").asText();
    }

    private long createProject(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Campus Map\", \"description\": \"Map of AUM\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asLong();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}

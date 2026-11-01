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
class ProjectApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginAndCreateProject() throws Exception {
        String token = registerAndLogin("alice@aum.edu");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Grade Tracker", "description": "Track grades",
                                 "category": "Web", "requiredSkills": "Java, Spring Boot"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Grade Tracker"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.ownerName").value("Test User"));

        mockMvc.perform(get("/api/projects").param("skill", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createProjectWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"X\", \"description\": \"Y\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProjectWithBlankTitleReturns400() throws Exception {
        String token = registerAndLogin("alice@aum.edu");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\", \"description\": \"Y\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    void onlyOwnerCanUpdateOrDeleteProject() throws Exception {
        String ownerToken = registerAndLogin("owner@aum.edu");
        String otherToken = registerAndLogin("other@aum.edu");
        long projectId = createProject(ownerToken);
        String update = "{\"title\": \"Changed\", \"description\": \"Changed\"}";

        mockMvc.perform(put("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Changed"));

        mockMvc.perform(delete("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/" + projectId))
                .andExpect(status().isNotFound());
    }

    @Test
    void registeringSameEmailTwiceReturns409() throws Exception {
        register("alice@aum.edu");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("ALICE@aum.edu")))
                .andExpect(status().isConflict());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        register("alice@aum.edu");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"alice@aum.edu\", \"password\": \"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanUpdateOwnProfileButNotOthers() throws Exception {
        String aliceToken = registerAndLogin("alice@aum.edu");
        String bobToken = registerAndLogin("bob@aum.edu");

        MvcResult me = mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@aum.edu"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        long aliceId = readJson(me).get("id").asLong();

        String update = "{\"name\": \"Alice B\", \"skills\": \"Java, SQL\", \"bio\": \"CS grad student\"}";

        mockMvc.perform(put("/api/users/" + aliceId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills").value("Java, SQL"));

        mockMvc.perform(put("/api/users/" + aliceId)
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isForbidden());
    }

    private String registerBody(String email) {
        return """
                {"name": "Test User", "email": "%s", "password": "password123",
                 "role": "STUDENT", "department": "Computer Science"}
                """.formatted(email);
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());
    }

    private String registerAndLogin(String email) throws Exception {
        register(email);

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

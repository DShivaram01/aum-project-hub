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
class UpvoteTipApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void upvotingIsOncePerUserAndCanBeUndone() throws Exception {
        String ownerToken = registerAndLogin("owner@aum.edu");
        String voterToken = registerAndLogin("voter@aum.edu");
        long projectId = createProject(ownerToken, "Campus Map");

        mockMvc.perform(put("/api/projects/" + projectId + "/upvote").header("Authorization", "Bearer " + voterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvotes").value(1))
                .andExpect(jsonPath("$.upvotedByMe").value(true));

        mockMvc.perform(put("/api/projects/" + projectId + "/upvote").header("Authorization", "Bearer " + voterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvotes").value(1));

        mockMvc.perform(get("/api/projects/" + projectId))
                .andExpect(jsonPath("$.upvotes").value(1))
                .andExpect(jsonPath("$.upvotedByMe").value(false));

        mockMvc.perform(delete("/api/projects/" + projectId + "/upvote")
                        .header("Authorization", "Bearer " + voterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvotes").value(0))
                .andExpect(jsonPath("$.upvotedByMe").value(false));
    }

    @Test
    void facultyAndStudentsCanBothUpvote() throws Exception {
        String studentToken = registerAndLogin("student@aum.edu");
        String facultyToken = registerAndLogin("prof@aum.edu", "FACULTY");
        long studentProject = createProject(studentToken, "Student idea");
        long facultyProject = createProject(facultyToken, "Faculty idea");

        mockMvc.perform(put("/api/projects/" + studentProject + "/upvote")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvotes").value(1));

        mockMvc.perform(put("/api/projects/" + facultyProject + "/upvote")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvotes").value(1));
    }

    @Test
    void upvoteRules() throws Exception {
        String ownerToken = registerAndLogin("owner@aum.edu");
        long projectId = createProject(ownerToken, "Campus Map");

        mockMvc.perform(put("/api/projects/" + projectId + "/upvote").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/projects/" + projectId + "/upvote"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/projects/9999/upvote").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void threeUpvotesMakeAProjectHotAndHotSortRanksIt() throws Exception {
        String ownerToken = registerAndLogin("owner@aum.edu");
        long popular = createProject(ownerToken, "Popular");
        createProject(ownerToken, "Newest but quiet");

        for (String email : new String[]{"a@aum.edu", "b@aum.edu", "c@aum.edu"}) {
            String token = registerAndLogin(email);
            mockMvc.perform(put("/api/projects/" + popular + "/upvote").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/projects"))
                .andExpect(jsonPath("$[0].title").value("Newest but quiet"));

        mockMvc.perform(get("/api/projects?sort=hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Popular"))
                .andExpect(jsonPath("$[0].upvotes").value(3))
                .andExpect(jsonPath("$[0].hot").value(true))
                .andExpect(jsonPath("$[1].hot").value(false));

        mockMvc.perform(get("/api/projects?sort=random"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anyoneCanReadTipsButPostingNeedsLogin() throws Exception {
        String ownerToken = registerAndLogin("owner@aum.edu");
        String helperToken = registerAndLogin("helper@aum.edu");
        long projectId = createProject(ownerToken, "Campus Map");

        mockMvc.perform(post("/api/projects/" + projectId + "/tips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Use Leaflet\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/projects/" + projectId + "/tips")
                        .header("Authorization", "Bearer " + helperToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"  Use Leaflet for the map tiles  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Use Leaflet for the map tiles"));

        mockMvc.perform(post("/api/projects/" + projectId + "/tips")
                        .header("Authorization", "Bearer " + helperToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.content").exists());

        mockMvc.perform(get("/api/projects/" + projectId + "/tips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].authorName").value("Test User"));
    }

    @Test
    void authorOrProjectOwnerCanDeleteATip() throws Exception {
        String ownerToken = registerAndLogin("owner@aum.edu");
        String helperToken = registerAndLogin("helper@aum.edu");
        String strangerToken = registerAndLogin("stranger@aum.edu");
        long projectId = createProject(ownerToken, "Campus Map");
        long first = addTip(projectId, helperToken);
        long second = addTip(projectId, helperToken);

        mockMvc.perform(delete("/api/tips/" + first).header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/tips/" + first).header("Authorization", "Bearer " + helperToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/tips/" + second).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/" + projectId + "/tips"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deletingProjectRemovesUpvotesAndTips() throws Exception {
        String ownerToken = registerAndLogin("owner@aum.edu");
        String helperToken = registerAndLogin("helper@aum.edu");
        long projectId = createProject(ownerToken, "Campus Map");
        addTip(projectId, helperToken);
        mockMvc.perform(put("/api/projects/" + projectId + "/upvote").header("Authorization", "Bearer " + helperToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/projects/" + projectId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/" + projectId + "/tips"))
                .andExpect(status().isNotFound());
    }

    private long addTip(long projectId, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects/" + projectId + "/tips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Have you tried caching?\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asLong();
    }

    private String registerAndLogin(String email) throws Exception {
        return registerAndLogin(email, "STUDENT");
    }

    private String registerAndLogin(String email, String role) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Test User", "email": "%s", "password": "password123",
                                 "role": "%s", "department": "Computer Science"}
                                """.formatted(email, role)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"password123\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).get("token").asText();
    }

    private long createProject(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"%s\", \"description\": \"A project\"}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asLong();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}

package com.aum.projecthub.demo;

import com.aum.projecthub.repository.JoinRequestRepository;
import com.aum.projecthub.repository.ProjectRepository;
import com.aum.projecthub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.demo-data=true")
@AutoConfigureMockMvc
@DirtiesContext
class DemoDataLoaderTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoDataLoader loader;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JoinRequestRepository joinRequestRepository;

    @Test
    void seedsDemoDataOnceAndDemoLoginWorks() throws Exception {
        long users = userRepository.count();
        long projects = projectRepository.count();
        long requests = joinRequestRepository.count();
        assertEquals(5, users);
        assertEquals(6, projects);
        assertEquals(6, requests);

        loader.run(null);
        assertEquals(users, userRepository.count());
        assertEquals(projects, projectRepository.count());
        assertEquals(requests, joinRequestRepository.count());

        mockMvc.perform(get("/api/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts.length()").value(2));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}"
                                .formatted(DemoDataLoader.FACULTY_EMAIL, DemoDataLoader.PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("FACULTY"));
    }
}

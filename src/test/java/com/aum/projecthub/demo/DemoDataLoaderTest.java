package com.aum.projecthub.demo;

import com.aum.projecthub.repository.EventRepository;
import com.aum.projecthub.repository.EventSubmissionRepository;
import com.aum.projecthub.repository.JoinRequestRepository;
import com.aum.projecthub.repository.ProjectRepository;
import com.aum.projecthub.repository.TipRepository;
import com.aum.projecthub.repository.UpvoteRepository;
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

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventSubmissionRepository submissionRepository;

    @Autowired
    private UpvoteRepository upvoteRepository;

    @Autowired
    private TipRepository tipRepository;

    @Test
    void seedsDemoDataOnceAndDemoLoginWorks() throws Exception {
        long users = userRepository.count();
        long projects = projectRepository.count();
        long requests = joinRequestRepository.count();
        assertEquals(5, users);
        assertEquals(7, projects);
        assertEquals(6, requests);
        assertEquals(3, eventRepository.count());
        assertEquals(5, submissionRepository.count());
        assertEquals(10, upvoteRepository.count());
        assertEquals(4, tipRepository.count());

        loader.run(null);
        assertEquals(users, userRepository.count());
        assertEquals(projects, projectRepository.count());
        assertEquals(requests, joinRequestRepository.count());
        assertEquals(3, eventRepository.count());
        assertEquals(10, upvoteRepository.count());

        mockMvc.perform(get("/api/projects?sort=hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Course Registration Waitlist Tracker"))
                .andExpect(jsonPath("$[0].hot").value(true));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("Fall Research Showcase"));

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

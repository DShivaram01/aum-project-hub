package com.aum.projecthub.controller;

import com.aum.projecthub.model.Event;
import com.aum.projecthub.repository.EventRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void facultyCreatesEventAndAnyoneCanListIt() throws Exception {
        String facultyToken = registerAndLogin("prof@aum.edu", "FACULTY");

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Research Showcase", 20, 10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.acceptingSubmissions").value(true))
                .andExpect(jsonPath("$.submissionCount").value(0));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Research Showcase"));
    }

    @Test
    void upcomingEventsAreSortedAndPastEventsAreHidden() throws Exception {
        String facultyToken = registerAndLogin("prof@aum.edu", "FACULTY");
        createEvent(facultyToken, "Later", 30, 20);
        createEvent(facultyToken, "Sooner", 5, 2);
        long past = createEvent(facultyToken, "Already happened", 5, 2);
        Event event = eventRepository.findById(past).orElseThrow();
        event.setStartsAt(Instant.now().minus(1, ChronoUnit.DAYS));
        event.setSubmissionDeadline(Instant.now().minus(3, ChronoUnit.DAYS));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Sooner"))
                .andExpect(jsonPath("$[1].title").value("Later"));
    }

    @Test
    void studentsCannotCreateEvents() throws Exception {
        String studentToken = registerAndLogin("student@aum.edu", "STUDENT");

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Student Event", 20, 10)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deadlineMustComeBeforeTheEvent() throws Exception {
        String facultyToken = registerAndLogin("prof@aum.edu", "FACULTY");

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Backwards", 5, 10)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Deadline passed", 5, -1)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"No dates\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.startsAt").exists());
    }

    @Test
    void studentEntersOwnProjectToPresentAndPublish() throws Exception {
        String facultyToken = registerAndLogin("prof@aum.edu", "FACULTY");
        String studentToken = registerAndLogin("student@aum.edu", "STUDENT");
        long eventId = createEvent(facultyToken, "Showcase", 20, 10);
        long projectId = createProject(studentToken);

        MvcResult result = mockMvc.perform(post("/api/projects/" + projectId + "/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionJson(eventId, true, true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.present").value(true))
                .andExpect(jsonPath("$.publish").value(true))
                .andExpect(jsonPath("$.eventTitle").value("Showcase"))
                .andReturn();
        long submissionId = readJson(result).get("id").asLong();

        mockMvc.perform(post("/api/projects/" + projectId + "/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionJson(eventId, true, false)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/events/" + eventId + "/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectTitle").value("Campus Map"));

        mockMvc.perform(get("/api/projects/" + projectId + "/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/events/" + eventId))
                .andExpect(jsonPath("$.submissionCount").value(1));

        mockMvc.perform(delete("/api/submissions/" + submissionId)
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/submissions/" + submissionId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void submissionRules() throws Exception {
        String facultyToken = registerAndLogin("prof@aum.edu", "FACULTY");
        String studentToken = registerAndLogin("student@aum.edu", "STUDENT");
        long eventId = createEvent(facultyToken, "Showcase", 20, 10);
        long projectId = createProject(studentToken);

        // not the owner
        mockMvc.perform(post("/api/projects/" + projectId + "/submissions")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionJson(eventId, true, false)))
                .andExpect(status().isForbidden());

        // neither present nor publish
        mockMvc.perform(post("/api/projects/" + projectId + "/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionJson(eventId, false, false)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/projects/" + projectId + "/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionJson(9999, true, false)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/projects/" + projectId + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionJson(eventId, true, false)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cannotSubmitAfterDeadline() throws Exception {
        String facultyToken = registerAndLogin("prof@aum.edu", "FACULTY");
        String studentToken = registerAndLogin("student@aum.edu", "STUDENT");
        long eventId = createEvent(facultyToken, "Showcase", 20, 10);
        long projectId = createProject(studentToken);
        eventRepository.findById(eventId).orElseThrow()
                .setSubmissionDeadline(Instant.now().minus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/projects/" + projectId + "/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionJson(eventId, true, false)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/events/" + eventId))
                .andExpect(jsonPath("$.acceptingSubmissions").value(false));
    }

    @Test
    void onlyCreatorCanDeleteEventAndItsEntriesGoWithIt() throws Exception {
        String facultyToken = registerAndLogin("prof@aum.edu", "FACULTY");
        String otherFacultyToken = registerAndLogin("prof2@aum.edu", "FACULTY");
        String studentToken = registerAndLogin("student@aum.edu", "STUDENT");
        long eventId = createEvent(facultyToken, "Showcase", 20, 10);
        long projectId = createProject(studentToken);
        mockMvc.perform(post("/api/projects/" + projectId + "/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submissionJson(eventId, true, false)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/events/" + eventId).header("Authorization", "Bearer " + otherFacultyToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/events/" + eventId).header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/" + projectId + "/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private String eventJson(String title, int startsInDays, int deadlineInDays) {
        Instant now = Instant.now();
        return """
                {"title": "%s", "description": "An event", "location": "Goodwyn Hall",
                 "startsAt": "%s", "submissionDeadline": "%s"}
                """.formatted(title, now.plus(startsInDays, ChronoUnit.DAYS),
                now.plus(deadlineInDays, ChronoUnit.DAYS));
    }

    private String submissionJson(long eventId, boolean present, boolean publish) {
        return "{\"eventId\": %d, \"present\": %b, \"publish\": %b}".formatted(eventId, present, publish);
    }

    private long createEvent(String token, String title, int startsInDays, int deadlineInDays) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(title, startsInDays, deadlineInDays)))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asLong();
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

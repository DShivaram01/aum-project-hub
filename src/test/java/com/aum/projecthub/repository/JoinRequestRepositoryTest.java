package com.aum.projecthub.repository;

import com.aum.projecthub.model.JoinRequest;
import com.aum.projecthub.model.Project;
import com.aum.projecthub.model.RequestStatus;
import com.aum.projecthub.model.Role;
import com.aum.projecthub.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class JoinRequestRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JoinRequestRepository joinRequestRepository;

    private User student;
    private Project project;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(new User("Dr. Smith", "smith@aum.edu", "pw", Role.FACULTY));
        student = userRepository.save(new User("Alice", "alice@aum.edu", "pw", Role.STUDENT));
        project = projectRepository.save(new Project("Grade Tracker", "Track grades", "Web", "Java", owner));
    }

    @Test
    void newRequestIsPending() {
        JoinRequest request = joinRequestRepository.save(new JoinRequest(project, student, "I know Java"));

        assertEquals(RequestStatus.PENDING, request.getStatus());
    }

    @Test
    void existsByProjectAndUserDetectsExistingRequest() {
        assertFalse(joinRequestRepository.existsByProjectIdAndUserId(project.getId(), student.getId()));

        joinRequestRepository.save(new JoinRequest(project, student, "Please add me"));

        assertTrue(joinRequestRepository.existsByProjectIdAndUserId(project.getId(), student.getId()));
        assertEquals(1, joinRequestRepository.findByProjectId(project.getId()).size());
    }

    @Test
    void duplicateRequestIsRejectedByDatabase() {
        joinRequestRepository.saveAndFlush(new JoinRequest(project, student, "First request"));

        assertThrows(DataIntegrityViolationException.class, () ->
                joinRequestRepository.saveAndFlush(new JoinRequest(project, student, "Second request")));
    }
}

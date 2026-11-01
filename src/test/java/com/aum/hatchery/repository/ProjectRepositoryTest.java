package com.aum.hatchery.repository;

import com.aum.hatchery.model.Project;
import com.aum.hatchery.model.ProjectStatus;
import com.aum.hatchery.model.Role;
import com.aum.hatchery.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
class ProjectRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new User("Dr. Smith", "smith@aum.edu", "pw", Role.FACULTY));

        projectRepository.save(new Project("Campus Map App", "Mobile map of AUM", "Mobile",
                "React Native, TypeScript", owner));
        projectRepository.save(new Project("Grade Tracker", "Track course grades", "Web",
                "Java, Spring Boot", owner));

        Project closed = new Project("Old Website", "Legacy site cleanup", "Web", "HTML, CSS", owner);
        closed.setStatus(ProjectStatus.CLOSED);
        projectRepository.save(closed);
    }

    @Test
    void newProjectIsOpenAndHasCreatedTime() {
        Project project = projectRepository.save(new Project("Chat Bot", "Help desk bot", "AI", "Python", owner));

        assertEquals(ProjectStatus.OPEN, project.getStatus());
        assertNotNull(project.getCreatedAt());
    }

    @Test
    void findByCategoryIgnoresCase() {
        List<Project> webProjects = projectRepository.findByCategoryIgnoreCase("web");

        assertEquals(2, webProjects.size());
    }

    @Test
    void findByStatusReturnsOnlyOpenProjects() {
        List<Project> openProjects = projectRepository.findByStatus(ProjectStatus.OPEN);

        assertEquals(2, openProjects.size());
    }

    @Test
    void findBySkillMatchesPartOfSkillList() {
        List<Project> javaProjects = projectRepository.findByRequiredSkillsContainingIgnoreCase("java");

        assertEquals(1, javaProjects.size());
        assertEquals("Grade Tracker", javaProjects.get(0).getTitle());
    }

    @Test
    void findByOwnerReturnsAllOwnerProjects() {
        assertEquals(3, projectRepository.findByOwnerId(owner.getId()).size());
    }
}

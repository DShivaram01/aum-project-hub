package com.aum.hatchery.service;

import com.aum.hatchery.dto.ProjectRequest;
import com.aum.hatchery.dto.ProjectResponse;
import com.aum.hatchery.exception.ProjectNotFoundException;
import com.aum.hatchery.exception.UnauthorizedActionException;
import com.aum.hatchery.model.Project;
import com.aum.hatchery.model.ProjectStatus;
import com.aum.hatchery.model.Role;
import com.aum.hatchery.model.User;
import com.aum.hatchery.repository.JoinRequestRepository;
import com.aum.hatchery.repository.ProjectRepository;
import com.aum.hatchery.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @InjectMocks
    private ProjectService projectService;

    private User owner;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = new User("Dr. Smith", "smith@aum.edu", "hashed", Role.FACULTY);
        owner.setId(1L);
        project = new Project("Grade Tracker", "Track grades", "Web", "Java", owner);
        project.setId(10L);
    }

    @Test
    void createProjectSetsLoggedInUserAsOwner() {
        when(userRepository.findByEmail("smith@aum.edu")).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectRequest request = new ProjectRequest("Campus Map", "Map of AUM", "Mobile", "React", null);
        ProjectResponse response = projectService.createProject(request, "smith@aum.edu");

        assertEquals(1L, response.ownerId());
        assertEquals(ProjectStatus.OPEN, response.status());
    }

    @Test
    void getProjectThrowsWhenMissing() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProjectNotFoundException.class, () -> projectService.getProject(99L));
    }

    @Test
    void ownerCanCloseProject() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectRequest request = new ProjectRequest("Grade Tracker", "Track grades", "Web", "Java",
                ProjectStatus.CLOSED);
        ProjectResponse response = projectService.updateProject(10L, request, "smith@aum.edu");

        assertEquals(ProjectStatus.CLOSED, response.status());
    }

    @Test
    void otherUserCannotUpdateProject() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        ProjectRequest request = new ProjectRequest("Hacked", "Hacked", null, null, null);

        assertThrows(UnauthorizedActionException.class,
                () -> projectService.updateProject(10L, request, "someone@aum.edu"));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void otherUserCannotDeleteProject() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThrows(UnauthorizedActionException.class,
                () -> projectService.deleteProject(10L, "someone@aum.edu"));
        verify(projectRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesJoinRequestsFirst() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        projectService.deleteProject(10L, "smith@aum.edu");

        verify(joinRequestRepository).deleteByProjectId(10L);
        verify(projectRepository).delete(project);
    }
}

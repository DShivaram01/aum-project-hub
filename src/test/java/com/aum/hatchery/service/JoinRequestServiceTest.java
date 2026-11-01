package com.aum.hatchery.service;

import com.aum.hatchery.dto.JoinRequestCreateRequest;
import com.aum.hatchery.dto.JoinRequestDecisionRequest;
import com.aum.hatchery.dto.JoinRequestResponse;
import com.aum.hatchery.exception.InvalidJoinRequestException;
import com.aum.hatchery.exception.JoinRequestConflictException;
import com.aum.hatchery.exception.UnauthorizedActionException;
import com.aum.hatchery.model.JoinRequest;
import com.aum.hatchery.model.Project;
import com.aum.hatchery.model.ProjectStatus;
import com.aum.hatchery.model.RequestStatus;
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
class JoinRequestServiceTest {

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JoinRequestService joinRequestService;

    private User owner;
    private User student;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = new User("Dr. Smith", "smith@aum.edu", "hashed", Role.FACULTY);
        owner.setId(1L);
        student = new User("Alice", "alice@aum.edu", "hashed", Role.STUDENT);
        student.setId(2L);
        project = new Project("Grade Tracker", "Track grades", "Web", "Java", owner);
        project.setId(10L);
    }

    @Test
    void studentCanRequestToJoinOpenProject() {
        when(userRepository.findByEmail("alice@aum.edu")).thenReturn(Optional.of(student));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JoinRequestResponse response = joinRequestService.createRequest(10L,
                new JoinRequestCreateRequest("I know Java"), "alice@aum.edu");

        assertEquals(RequestStatus.PENDING, response.status());
        assertEquals(2L, response.userId());
        assertEquals("I know Java", response.message());
    }

    @Test
    void ownerCannotRequestToJoinOwnProject() {
        when(userRepository.findByEmail("smith@aum.edu")).thenReturn(Optional.of(owner));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThrows(InvalidJoinRequestException.class,
                () -> joinRequestService.createRequest(10L, null, "smith@aum.edu"));
        verify(joinRequestRepository, never()).save(any());
    }

    @Test
    void cannotRequestToJoinClosedProject() {
        project.setStatus(ProjectStatus.CLOSED);
        when(userRepository.findByEmail("alice@aum.edu")).thenReturn(Optional.of(student));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThrows(InvalidJoinRequestException.class,
                () -> joinRequestService.createRequest(10L, null, "alice@aum.edu"));
    }

    @Test
    void secondRequestToSameProjectIsRejected() {
        when(userRepository.findByEmail("alice@aum.edu")).thenReturn(Optional.of(student));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(joinRequestRepository.existsByProjectIdAndUserId(10L, 2L)).thenReturn(true);

        assertThrows(JoinRequestConflictException.class,
                () -> joinRequestService.createRequest(10L, null, "alice@aum.edu"));
        verify(joinRequestRepository, never()).save(any());
    }

    @Test
    void ownerCanAcceptPendingRequest() {
        JoinRequest request = pendingRequest();
        when(joinRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JoinRequestResponse response = joinRequestService.decide(5L,
                new JoinRequestDecisionRequest(RequestStatus.ACCEPTED), "smith@aum.edu");

        assertEquals(RequestStatus.ACCEPTED, response.status());
    }

    @Test
    void nonOwnerCannotDecide() {
        when(joinRequestRepository.findById(5L)).thenReturn(Optional.of(pendingRequest()));

        assertThrows(UnauthorizedActionException.class, () -> joinRequestService.decide(5L,
                new JoinRequestDecisionRequest(RequestStatus.ACCEPTED), "alice@aum.edu"));
        verify(joinRequestRepository, never()).save(any());
    }

    @Test
    void cannotDecideTwice() {
        JoinRequest request = pendingRequest();
        request.setStatus(RequestStatus.REJECTED);
        when(joinRequestRepository.findById(5L)).thenReturn(Optional.of(request));

        assertThrows(JoinRequestConflictException.class, () -> joinRequestService.decide(5L,
                new JoinRequestDecisionRequest(RequestStatus.ACCEPTED), "smith@aum.edu"));
    }

    @Test
    void pendingIsNotAValidDecision() {
        when(joinRequestRepository.findById(5L)).thenReturn(Optional.of(pendingRequest()));

        assertThrows(InvalidJoinRequestException.class, () -> joinRequestService.decide(5L,
                new JoinRequestDecisionRequest(RequestStatus.PENDING), "smith@aum.edu"));
    }

    @Test
    void onlyRequesterCanWithdraw() {
        JoinRequest request = pendingRequest();
        when(joinRequestRepository.findById(5L)).thenReturn(Optional.of(request));

        assertThrows(UnauthorizedActionException.class, () -> joinRequestService.withdraw(5L, "smith@aum.edu"));

        joinRequestService.withdraw(5L, "alice@aum.edu");
        verify(joinRequestRepository).delete(request);
    }

    @Test
    void cannotWithdrawAcceptedRequest() {
        JoinRequest request = pendingRequest();
        request.setStatus(RequestStatus.ACCEPTED);
        when(joinRequestRepository.findById(5L)).thenReturn(Optional.of(request));

        assertThrows(JoinRequestConflictException.class, () -> joinRequestService.withdraw(5L, "alice@aum.edu"));
        verify(joinRequestRepository, never()).delete(any());
    }

    private JoinRequest pendingRequest() {
        JoinRequest request = new JoinRequest(project, student, "Please add me");
        request.setId(5L);
        return request;
    }
}

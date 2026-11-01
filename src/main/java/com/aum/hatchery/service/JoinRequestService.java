package com.aum.hatchery.service;

import com.aum.hatchery.dto.JoinRequestCreateRequest;
import com.aum.hatchery.dto.JoinRequestDecisionRequest;
import com.aum.hatchery.dto.JoinRequestResponse;
import com.aum.hatchery.exception.InvalidJoinRequestException;
import com.aum.hatchery.exception.JoinRequestConflictException;
import com.aum.hatchery.exception.JoinRequestNotFoundException;
import com.aum.hatchery.exception.ProjectNotFoundException;
import com.aum.hatchery.exception.UnauthorizedActionException;
import com.aum.hatchery.exception.UserNotFoundException;
import com.aum.hatchery.model.JoinRequest;
import com.aum.hatchery.model.Project;
import com.aum.hatchery.model.ProjectStatus;
import com.aum.hatchery.model.RequestStatus;
import com.aum.hatchery.model.User;
import com.aum.hatchery.repository.JoinRequestRepository;
import com.aum.hatchery.repository.ProjectRepository;
import com.aum.hatchery.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JoinRequestService {

    private final JoinRequestRepository joinRequestRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public JoinRequestService(JoinRequestRepository joinRequestRepository, ProjectRepository projectRepository,
                              UserRepository userRepository) {
        this.joinRequestRepository = joinRequestRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public JoinRequestResponse createRequest(Long projectId, JoinRequestCreateRequest request,
                                             String currentUserEmail) {
        User user = findUserByEmail(currentUserEmail);
        Project project = findProjectById(projectId);

        if (project.getOwner().getId().equals(user.getId())) {
            throw new InvalidJoinRequestException("You cannot request to join your own project");
        }
        if (project.getStatus() != ProjectStatus.OPEN) {
            throw new InvalidJoinRequestException("This project is not accepting new members");
        }
        if (joinRequestRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new JoinRequestConflictException("You have already requested to join this project");
        }

        String message = request == null ? null : request.message();
        return JoinRequestResponse.from(joinRequestRepository.save(new JoinRequest(project, user, message)));
    }

    @Transactional(readOnly = true)
    public List<JoinRequestResponse> getRequestsForProject(Long projectId, String currentUserEmail) {
        Project project = findProjectById(projectId);
        checkOwner(project, currentUserEmail, "Only the project owner can see its join requests");

        return joinRequestRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(JoinRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JoinRequestResponse> getMyRequests(String currentUserEmail) {
        User user = findUserByEmail(currentUserEmail);

        return joinRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(JoinRequestResponse::from)
                .toList();
    }

    @Transactional
    public JoinRequestResponse decide(Long requestId, JoinRequestDecisionRequest decision, String currentUserEmail) {
        JoinRequest joinRequest = findRequestById(requestId);
        checkOwner(joinRequest.getProject(), currentUserEmail,
                "Only the project owner can accept or reject requests");

        if (decision.status() == RequestStatus.PENDING) {
            throw new InvalidJoinRequestException("Status must be ACCEPTED or REJECTED");
        }
        if (joinRequest.getStatus() != RequestStatus.PENDING) {
            throw new JoinRequestConflictException(
                    "This request has already been " + joinRequest.getStatus().name().toLowerCase());
        }

        joinRequest.setStatus(decision.status());
        return JoinRequestResponse.from(joinRequestRepository.save(joinRequest));
    }

    @Transactional
    public void withdraw(Long requestId, String currentUserEmail) {
        JoinRequest joinRequest = findRequestById(requestId);

        if (!joinRequest.getUser().getEmail().equals(currentUserEmail)) {
            throw new UnauthorizedActionException("You can only withdraw your own requests");
        }
        if (joinRequest.getStatus() != RequestStatus.PENDING) {
            throw new JoinRequestConflictException("Only pending requests can be withdrawn");
        }

        joinRequestRepository.delete(joinRequest);
    }

    private void checkOwner(Project project, String currentUserEmail, String message) {
        if (!project.getOwner().getEmail().equals(currentUserEmail)) {
            throw new UnauthorizedActionException(message);
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
    }

    private Project findProjectById(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }

    private JoinRequest findRequestById(Long id) {
        return joinRequestRepository.findById(id).orElseThrow(() -> new JoinRequestNotFoundException(id));
    }
}

package com.aum.projecthub.controller;

import com.aum.projecthub.dto.JoinRequestCreateRequest;
import com.aum.projecthub.dto.JoinRequestDecisionRequest;
import com.aum.projecthub.dto.JoinRequestResponse;
import com.aum.projecthub.service.JoinRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class JoinRequestController {
    private final JoinRequestService joinRequestService;

    public JoinRequestController(JoinRequestService joinRequestService) {
        this.joinRequestService = joinRequestService;
    }

    @PostMapping("/api/projects/{projectId}/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public JoinRequestResponse createRequest(@PathVariable Long projectId,
                                             @Valid @RequestBody(required = false) JoinRequestCreateRequest request,
                                             Authentication authentication) {
        return joinRequestService.createRequest(projectId, request, authentication.getName());
    }

    @GetMapping("/api/projects/{projectId}/join-requests")
    public List<JoinRequestResponse> getRequestsForProject(@PathVariable Long projectId,
                                                           Authentication authentication) {
        return joinRequestService.getRequestsForProject(projectId, authentication.getName());
    }

    @GetMapping("/api/join-requests/mine")
    public List<JoinRequestResponse> getMyRequests(Authentication authentication) {
        return joinRequestService.getMyRequests(authentication.getName());
    }

    @PutMapping("/api/join-requests/{id}")
    public JoinRequestResponse decide(@PathVariable Long id, @Valid @RequestBody JoinRequestDecisionRequest decision,
                                      Authentication authentication) {
        return joinRequestService.decide(id, decision, authentication.getName());
    }

    @DeleteMapping("/api/join-requests/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@PathVariable Long id, Authentication authentication) {
        joinRequestService.withdraw(id, authentication.getName());
    }
}

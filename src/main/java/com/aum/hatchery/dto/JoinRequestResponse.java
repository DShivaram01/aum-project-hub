package com.aum.hatchery.dto;

import com.aum.hatchery.model.JoinRequest;
import com.aum.hatchery.model.RequestStatus;

import java.time.LocalDateTime;

public record JoinRequestResponse(
        Long id,
        Long projectId,
        String projectTitle,
        Long userId,
        String userName,
        String userEmail,
        String userSkills,
        String message,
        RequestStatus status,
        LocalDateTime createdAt) {

    public static JoinRequestResponse from(JoinRequest request) {
        return new JoinRequestResponse(request.getId(), request.getProject().getId(),
                request.getProject().getTitle(), request.getUser().getId(), request.getUser().getName(),
                request.getUser().getEmail(), request.getUser().getSkills(), request.getMessage(),
                request.getStatus(), request.getCreatedAt());
    }
}

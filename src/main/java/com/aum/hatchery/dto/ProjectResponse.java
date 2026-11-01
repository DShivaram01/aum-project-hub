package com.aum.hatchery.dto;

import com.aum.hatchery.model.Project;
import com.aum.hatchery.model.ProjectStatus;

import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String title,
        String description,
        String category,
        String requiredSkills,
        ProjectStatus status,
        Long ownerId,
        String ownerName,
        LocalDateTime createdAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getTitle(), project.getDescription(),
                project.getCategory(), project.getRequiredSkills(), project.getStatus(),
                project.getOwner().getId(), project.getOwner().getName(), project.getCreatedAt());
    }
}

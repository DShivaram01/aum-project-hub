package com.aum.projecthub.dto;

import com.aum.projecthub.model.Project;
import com.aum.projecthub.model.ProjectStatus;

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
        LocalDateTime createdAt,
        long upvotes,
        boolean hot,
        boolean upvotedByMe) {
    public static final int HOT_UPVOTES = 3;

    public static ProjectResponse from(Project project, long upvotes, boolean upvotedByMe) {
        return new ProjectResponse(project.getId(), project.getTitle(), project.getDescription(),
                project.getCategory(), project.getRequiredSkills(), project.getStatus(),
                project.getOwner().getId(), project.getOwner().getName(), project.getCreatedAt(),
                upvotes, upvotes >= HOT_UPVOTES, upvotedByMe);
    }
}

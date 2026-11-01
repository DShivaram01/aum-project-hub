package com.aum.projecthub.dto;

import com.aum.projecthub.model.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 2000) String description,
        @Size(max = 255) String category,
        @Size(max = 255) String requiredSkills,
        ProjectStatus status) {
}

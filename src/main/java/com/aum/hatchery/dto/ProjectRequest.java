package com.aum.hatchery.dto;

import com.aum.hatchery.model.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Used for both creating and updating a project.
// If status is left empty, a new project starts as OPEN.
public record ProjectRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 2000) String description,
        String category,
        String requiredSkills,
        ProjectStatus status) {
}

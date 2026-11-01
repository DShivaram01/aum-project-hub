package com.aum.projecthub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String department,
        @Size(max = 255) String skills,
        @Size(max = 1000) String bio) {
}

package com.aum.hatchery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Fields a user is allowed to change on their own profile.
// Email, password and role are not changed here.
public record UpdateUserRequest(
        @NotBlank String name,
        String department,
        String skills,
        @Size(max = 1000) String bio) {
}

package com.aum.projecthub.dto;

import com.aum.projecthub.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        // bcrypt ignores anything past 72 bytes
        @NotBlank @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") String password,
        @NotNull Role role,
        @Size(max = 255) String department) {
}

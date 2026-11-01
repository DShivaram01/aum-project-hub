package com.aum.projecthub.dto;

import com.aum.projecthub.model.Role;
import com.aum.projecthub.model.User;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        String department,
        String skills,
        String bio) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getDepartment(), user.getSkills(), user.getBio());
    }
}

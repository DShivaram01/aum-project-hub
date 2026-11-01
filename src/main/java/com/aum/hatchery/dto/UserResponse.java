package com.aum.hatchery.dto;

import com.aum.hatchery.model.Role;
import com.aum.hatchery.model.User;

// What we send back to the client for a user. Leaves out the password.
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

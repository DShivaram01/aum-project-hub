package com.aum.hatchery.controller;

import com.aum.hatchery.dto.UpdateUserRequest;
import com.aum.hatchery.dto.UserResponse;
import com.aum.hatchery.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Profile of the logged-in user (handy for the frontend)
    @GetMapping("/me")
    public UserResponse getCurrentUser(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName());
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request,
                                   Authentication authentication) {
        return userService.updateUser(id, request, authentication.getName());
    }
}

package com.aum.hatchery.dto;

// Sent back after a successful login. The client sends the token in the Authorization header.
public record AuthResponse(String token, UserResponse user) {
}

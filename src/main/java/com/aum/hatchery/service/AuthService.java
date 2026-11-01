package com.aum.hatchery.service;

import com.aum.hatchery.config.JwtService;
import com.aum.hatchery.dto.AuthResponse;
import com.aum.hatchery.dto.LoginRequest;
import com.aum.hatchery.dto.RegisterRequest;
import com.aum.hatchery.dto.UserResponse;
import com.aum.hatchery.exception.EmailAlreadyUsedException;
import com.aum.hatchery.exception.InvalidCredentialsException;
import com.aum.hatchery.model.User;
import com.aum.hatchery.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {
        // Store emails in lowercase so "Alice@aum.edu" and "alice@aum.edu" count as the same account
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException("An account with this email already exists");
        }

        // Save a BCrypt hash of the password, never the password itself
        User user = new User(request.name(), email, passwordEncoder.encode(request.password()), request.role());
        user.setDepartment(request.department());

        return UserResponse.from(userRepository.save(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        // Use the same message for "no such user" and "wrong password"
        // so nobody can find out which emails are registered
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }
}

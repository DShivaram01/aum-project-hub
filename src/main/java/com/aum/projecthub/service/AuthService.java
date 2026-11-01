package com.aum.projecthub.service;

import com.aum.projecthub.config.JwtService;
import com.aum.projecthub.dto.AuthResponse;
import com.aum.projecthub.dto.LoginRequest;
import com.aum.projecthub.dto.RegisterRequest;
import com.aum.projecthub.dto.UserResponse;
import com.aum.projecthub.exception.ConflictException;
import com.aum.projecthub.exception.InvalidCredentialsException;
import com.aum.projecthub.model.User;
import com.aum.projecthub.repository.UserRepository;
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
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User(request.name(), email, passwordEncoder.encode(request.password()), request.role());
        user.setDepartment(request.department());

        return UserResponse.from(userRepository.save(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }
}

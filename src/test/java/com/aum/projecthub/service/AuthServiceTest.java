package com.aum.projecthub.service;

import com.aum.projecthub.config.JwtService;
import com.aum.projecthub.dto.AuthResponse;
import com.aum.projecthub.dto.LoginRequest;
import com.aum.projecthub.dto.RegisterRequest;
import com.aum.projecthub.dto.UserResponse;
import com.aum.projecthub.exception.ConflictException;
import com.aum.projecthub.exception.InvalidCredentialsException;
import com.aum.projecthub.model.Role;
import com.aum.projecthub.model.User;
import com.aum.projecthub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerSavesHashedPasswordAndLowercaseEmail() {
        RegisterRequest request = new RegisterRequest("Alice", "Alice@AUM.edu", "password123", Role.STUDENT, "CS");
        when(userRepository.existsByEmail("alice@aum.edu")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(request);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("hashed", savedUser.getValue().getPassword());
        assertEquals("alice@aum.edu", savedUser.getValue().getEmail());
        assertEquals("alice@aum.edu", response.email());
    }

    @Test
    void registerFailsWhenEmailIsTaken() {
        RegisterRequest request = new RegisterRequest("Alice", "alice@aum.edu", "password123", Role.STUDENT, null);
        when(userRepository.existsByEmail("alice@aum.edu")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokenForCorrectPassword() {
        User user = new User("Alice", "alice@aum.edu", "hashed", Role.STUDENT);
        when(userRepository.findByEmail("alice@aum.edu")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken("alice@aum.edu")).thenReturn("fake-token");

        AuthResponse response = authService.login(new LoginRequest("alice@aum.edu", "password123"));

        assertEquals("fake-token", response.token());
        assertEquals("Alice", response.user().name());
    }

    @Test
    void loginFailsForWrongPassword() {
        User user = new User("Alice", "alice@aum.edu", "hashed", Role.STUDENT);
        when(userRepository.findByEmail("alice@aum.edu")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("alice@aum.edu", "wrong")));
    }

    @Test
    void loginFailsForUnknownEmail() {
        when(userRepository.findByEmail("nobody@aum.edu")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("nobody@aum.edu", "password123")));
    }
}

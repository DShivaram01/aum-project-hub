package com.aum.projecthub.service;

import com.aum.projecthub.dto.UpdateUserRequest;
import com.aum.projecthub.dto.UserResponse;
import com.aum.projecthub.exception.ForbiddenException;
import com.aum.projecthub.exception.NotFoundException;
import com.aum.projecthub.model.User;
import com.aum.projecthub.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found with id " + id));
        return UserResponse.from(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found: " + email));
        return UserResponse.from(user);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request, String currentUserEmail) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found with id " + id));

        if (!user.getEmail().equals(currentUserEmail)) {
            throw new ForbiddenException("You can only update your own profile");
        }

        user.setName(request.name());
        user.setDepartment(request.department());
        user.setSkills(request.skills());
        user.setBio(request.bio());

        return UserResponse.from(userRepository.save(user));
    }
}

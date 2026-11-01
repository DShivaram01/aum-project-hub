package com.aum.hatchery.service;

import com.aum.hatchery.dto.UpdateUserRequest;
import com.aum.hatchery.dto.UserResponse;
import com.aum.hatchery.exception.UnauthorizedActionException;
import com.aum.hatchery.exception.UserNotFoundException;
import com.aum.hatchery.model.User;
import com.aum.hatchery.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        return UserResponse.from(user);
    }

    // The email comes from the JWT token, so the user must exist
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
        return UserResponse.from(user);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request, String currentUserEmail) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));

        // Users can only edit their own profile
        if (!user.getEmail().equals(currentUserEmail)) {
            throw new UnauthorizedActionException("You can only update your own profile");
        }

        user.setName(request.name());
        user.setDepartment(request.department());
        user.setSkills(request.skills());
        user.setBio(request.bio());

        return UserResponse.from(userRepository.save(user));
    }
}

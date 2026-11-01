package com.aum.hatchery.repository;

import com.aum.hatchery.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // Used during registration to make sure the email is not taken
    boolean existsByEmail(String email);
}

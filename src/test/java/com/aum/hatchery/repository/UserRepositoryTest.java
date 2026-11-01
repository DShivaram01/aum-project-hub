package com.aum.hatchery.repository;

import com.aum.hatchery.model.Role;
import com.aum.hatchery.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindUserByEmail() {
        User user = new User("Alice", "alice@aum.edu", "hashed-password", Role.STUDENT);
        user.setSkills("Java, SQL");
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("alice@aum.edu");

        assertTrue(found.isPresent());
        assertEquals("Alice", found.get().getName());
        assertEquals(Role.STUDENT, found.get().getRole());
    }

    @Test
    void existsByEmailReturnsFalseForUnknownEmail() {
        assertFalse(userRepository.existsByEmail("nobody@aum.edu"));
    }

    @Test
    void duplicateEmailIsRejectedByDatabase() {
        userRepository.saveAndFlush(new User("Alice", "same@aum.edu", "pw1", Role.STUDENT));

        // Second user with the same email should break the unique constraint
        assertThrows(DataIntegrityViolationException.class, () ->
                userRepository.saveAndFlush(new User("Bob", "same@aum.edu", "pw2", Role.FACULTY)));
    }
}

package com.aum.hatchery.repository;

import com.aum.hatchery.model.Project;
import com.aum.hatchery.model.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Spring Data builds the SQL for these methods from the method names
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByCategoryIgnoreCase(String category);

    List<Project> findByStatus(ProjectStatus status);

    List<Project> findByOwnerId(Long ownerId);

    // Matches projects whose skill list contains the given skill, e.g. "java" matches "Java, SQL"
    List<Project> findByRequiredSkillsContainingIgnoreCase(String skill);
}

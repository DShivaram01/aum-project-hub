package com.aum.hatchery.repository;

import com.aum.hatchery.model.Project;
import com.aum.hatchery.model.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwnerId(Long ownerId);

    // Each filter is optional. An empty category or skill means "don't filter on this",
    // and a null status means any status.
    // Category and skill must already be lowercase. We use '' instead of null for them because
    // PostgreSQL cannot work out the type of a null parameter inside LOWER() or LIKE.
    @Query("SELECT p FROM Project p "
            + "WHERE (:category = '' OR LOWER(p.category) = :category) "
            + "AND (:status IS NULL OR p.status = :status) "
            + "AND (:skill = '' OR LOWER(p.requiredSkills) LIKE CONCAT('%', :skill, '%')) "
            + "ORDER BY p.createdAt DESC")
    List<Project> search(@Param("category") String category,
                         @Param("status") ProjectStatus status,
                         @Param("skill") String skill);
}

package com.aum.projecthub.repository;

import com.aum.projecthub.model.Project;
import com.aum.projecthub.model.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwnerId(Long ownerId);

    // using '' instead of null for "no filter" - postgres can't infer the type of a null param inside LOWER()
    @Query("SELECT p FROM Project p "
            + "WHERE (:category = '' OR LOWER(p.category) = :category) "
            + "AND (:status IS NULL OR p.status = :status) "
            + "AND (:skill = '' OR LOWER(p.requiredSkills) LIKE CONCAT('%', :skill, '%')) "
            + "ORDER BY p.createdAt DESC")
    List<Project> search(@Param("category") String category,
                         @Param("status") ProjectStatus status,
                         @Param("skill") String skill);
}

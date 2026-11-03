package com.aum.projecthub.repository;

import com.aum.projecthub.model.Upvote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UpvoteRepository extends JpaRepository<Upvote, Long> {
    long countByProjectId(Long projectId);

    Optional<Upvote> findByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserEmail(Long projectId, String email);

    // rows of [projectId, count]
    @Query("SELECT u.project.id, COUNT(u) FROM Upvote u GROUP BY u.project.id")
    List<Object[]> countPerProject();

    @Query("SELECT u.project.id FROM Upvote u WHERE u.user.email = :email")
    List<Long> findProjectIdsUpvotedBy(@Param("email") String email);

    void deleteByProjectId(Long projectId);
}

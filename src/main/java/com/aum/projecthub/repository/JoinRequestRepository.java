package com.aum.projecthub.repository;

import com.aum.projecthub.model.JoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {
    List<JoinRequest> findByProjectId(Long projectId);

    List<JoinRequest> findByUserId(Long userId);

    List<JoinRequest> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<JoinRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    void deleteByProjectId(Long projectId);
}

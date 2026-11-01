package com.aum.hatchery.repository;

import com.aum.hatchery.model.JoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {

    List<JoinRequest> findByProjectId(Long projectId);

    List<JoinRequest> findByUserId(Long userId);

    // Used to stop a user from sending a second request to the same project
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
}

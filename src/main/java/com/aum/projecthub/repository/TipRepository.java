package com.aum.projecthub.repository;

import com.aum.projecthub.model.Tip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipRepository extends JpaRepository<Tip, Long> {
    List<Tip> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    void deleteByProjectId(Long projectId);
}

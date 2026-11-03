package com.aum.projecthub.repository;

import com.aum.projecthub.model.EventSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventSubmissionRepository extends JpaRepository<EventSubmission, Long> {
    List<EventSubmission> findByEventIdOrderByCreatedAtAsc(Long eventId);

    List<EventSubmission> findByProjectIdOrderByCreatedAtAsc(Long projectId);

    long countByEventId(Long eventId);

    boolean existsByEventIdAndProjectId(Long eventId, Long projectId);

    // rows of [eventId, count]
    @Query("SELECT s.event.id, COUNT(s) FROM EventSubmission s GROUP BY s.event.id")
    List<Object[]> countPerEvent();

    void deleteByEventId(Long eventId);

    void deleteByProjectId(Long projectId);
}

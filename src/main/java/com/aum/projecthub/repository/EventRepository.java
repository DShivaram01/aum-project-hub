package com.aum.projecthub.repository;

import com.aum.projecthub.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByStartsAtAfterOrderByStartsAtAsc(Instant time);
}

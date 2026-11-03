package com.aum.projecthub.service;

import com.aum.projecthub.dto.EventRequest;
import com.aum.projecthub.dto.EventResponse;
import com.aum.projecthub.exception.BadRequestException;
import com.aum.projecthub.exception.ForbiddenException;
import com.aum.projecthub.exception.NotFoundException;
import com.aum.projecthub.model.Event;
import com.aum.projecthub.model.Role;
import com.aum.projecthub.model.User;
import com.aum.projecthub.repository.EventRepository;
import com.aum.projecthub.repository.EventSubmissionRepository;
import com.aum.projecthub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EventService {
    private final EventRepository eventRepository;
    private final EventSubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public EventService(EventRepository eventRepository, EventSubmissionRepository submissionRepository,
                        UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : submissionRepository.countPerEvent()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return eventRepository.findByStartsAtAfterOrderByStartsAtAsc(Instant.now()).stream()
                .map(event -> EventResponse.from(event, counts.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(Long id) {
        return toResponse(findEventById(id));
    }

    @Transactional
    public EventResponse createEvent(EventRequest request, String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("User not found: " + currentUserEmail));
        if (user.getRole() != Role.FACULTY) {
            throw new ForbiddenException("Only faculty can create events");
        }
        checkDates(request);
        if (!request.submissionDeadline().isAfter(Instant.now())) {
            throw new BadRequestException("Submission deadline must be in the future");
        }

        Event event = new Event(request.title(), request.description(), request.location(), request.startsAt(),
                request.submissionDeadline(), user);
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request, String currentUserEmail) {
        Event event = findEventById(id);
        checkCreator(event, currentUserEmail);
        checkDates(request);

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setLocation(request.location());
        event.setStartsAt(request.startsAt());
        event.setSubmissionDeadline(request.submissionDeadline());
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public void deleteEvent(Long id, String currentUserEmail) {
        Event event = findEventById(id);
        checkCreator(event, currentUserEmail);

        submissionRepository.deleteByEventId(id);
        eventRepository.delete(event);
    }

    Event findEventById(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Event not found with id " + id));
    }

    private EventResponse toResponse(Event event) {
        return EventResponse.from(event, submissionRepository.countByEventId(event.getId()));
    }

    private void checkDates(EventRequest request) {
        if (request.submissionDeadline().isAfter(request.startsAt())) {
            throw new BadRequestException("Submission deadline must be before the event starts");
        }
    }

    private void checkCreator(Event event, String currentUserEmail) {
        if (!event.getCreatedBy().getEmail().equals(currentUserEmail)) {
            throw new ForbiddenException("Only the person who created this event can change it");
        }
    }
}

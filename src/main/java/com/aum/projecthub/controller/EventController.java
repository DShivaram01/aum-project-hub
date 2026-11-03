package com.aum.projecthub.controller;

import com.aum.projecthub.dto.EventRequest;
import com.aum.projecthub.dto.EventResponse;
import com.aum.projecthub.dto.SubmissionResponse;
import com.aum.projecthub.service.EventService;
import com.aum.projecthub.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;
    private final SubmissionService submissionService;

    public EventController(EventService eventService, SubmissionService submissionService) {
        this.eventService = eventService;
        this.submissionService = submissionService;
    }

    @GetMapping
    public List<EventResponse> getUpcomingEvents() {
        return eventService.getUpcomingEvents();
    }

    @GetMapping("/{id}")
    public EventResponse getEvent(@PathVariable Long id) {
        return eventService.getEvent(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(@Valid @RequestBody EventRequest request, Authentication authentication) {
        return eventService.createEvent(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public EventResponse updateEvent(@PathVariable Long id, @Valid @RequestBody EventRequest request,
                                     Authentication authentication) {
        return eventService.updateEvent(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable Long id, Authentication authentication) {
        eventService.deleteEvent(id, authentication.getName());
    }

    @GetMapping("/{id}/submissions")
    public List<SubmissionResponse> getSubmissions(@PathVariable Long id) {
        return submissionService.getForEvent(id);
    }
}

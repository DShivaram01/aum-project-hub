package com.aum.projecthub.dto;

import com.aum.projecthub.model.Event;

import java.time.Instant;

public record EventResponse(
        Long id,
        String title,
        String description,
        String location,
        Instant startsAt,
        Instant submissionDeadline,
        boolean acceptingSubmissions,
        Long createdById,
        String createdByName,
        long submissionCount) {
    public static EventResponse from(Event event, long submissionCount) {
        return new EventResponse(event.getId(), event.getTitle(), event.getDescription(), event.getLocation(),
                event.getStartsAt(), event.getSubmissionDeadline(), event.isAcceptingSubmissions(),
                event.getCreatedBy().getId(), event.getCreatedBy().getName(), submissionCount);
    }
}

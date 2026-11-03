package com.aum.projecthub.dto;

import com.aum.projecthub.model.EventSubmission;

import java.time.Instant;

public record SubmissionResponse(
        Long id,
        Long eventId,
        String eventTitle,
        Instant eventStartsAt,
        Instant submissionDeadline,
        Long projectId,
        String projectTitle,
        Long ownerId,
        String ownerName,
        boolean present,
        boolean publish,
        Instant createdAt) {
    public static SubmissionResponse from(EventSubmission submission) {
        var event = submission.getEvent();
        var project = submission.getProject();
        return new SubmissionResponse(submission.getId(), event.getId(), event.getTitle(), event.getStartsAt(),
                event.getSubmissionDeadline(), project.getId(), project.getTitle(), project.getOwner().getId(),
                project.getOwner().getName(), submission.isPresent(), submission.isPublish(),
                submission.getCreatedAt());
    }
}

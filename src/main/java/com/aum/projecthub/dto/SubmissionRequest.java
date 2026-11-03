package com.aum.projecthub.dto;

import jakarta.validation.constraints.NotNull;

public record SubmissionRequest(
        @NotNull Long eventId,
        boolean present,
        boolean publish) {
}

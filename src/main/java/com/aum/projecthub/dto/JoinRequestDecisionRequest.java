package com.aum.projecthub.dto;

import com.aum.projecthub.model.RequestStatus;
import jakarta.validation.constraints.NotNull;

public record JoinRequestDecisionRequest(
        @NotNull RequestStatus status) {
}

package com.aum.hatchery.dto;

import com.aum.hatchery.model.RequestStatus;
import jakarta.validation.constraints.NotNull;

// The project owner sends ACCEPTED or REJECTED
public record JoinRequestDecisionRequest(
        @NotNull RequestStatus status) {
}

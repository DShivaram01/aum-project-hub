package com.aum.projecthub.dto;

import jakarta.validation.constraints.Size;

public record JoinRequestCreateRequest(
        @Size(max = 1000) String message) {
}

package com.aum.projecthub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipRequest(@NotBlank @Size(max = 1000) String content) {
}

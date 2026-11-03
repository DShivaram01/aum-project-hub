package com.aum.projecthub.dto;

import com.aum.projecthub.model.Role;
import com.aum.projecthub.model.Tip;

import java.time.LocalDateTime;

public record TipResponse(
        Long id,
        Long projectId,
        Long authorId,
        String authorName,
        Role authorRole,
        String content,
        LocalDateTime createdAt) {
    public static TipResponse from(Tip tip) {
        return new TipResponse(tip.getId(), tip.getProject().getId(), tip.getAuthor().getId(),
                tip.getAuthor().getName(), tip.getAuthor().getRole(), tip.getContent(), tip.getCreatedAt());
    }
}

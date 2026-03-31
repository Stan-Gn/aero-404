package com.aero.dto;

import java.time.LocalDateTime;

public record OperationCommentDto(
        Long id,
        String text,
        UserSimpleDto createdBy,
        LocalDateTime createdAt
) {
}

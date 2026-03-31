package com.aero.dto;

import java.time.LocalDateTime;

public record OperationHistoryDto(
        Long id,
        String fieldName,
        String oldValue,
        String newValue,
        UserSimpleDto changedBy,
        LocalDateTime changedAt
) {
}

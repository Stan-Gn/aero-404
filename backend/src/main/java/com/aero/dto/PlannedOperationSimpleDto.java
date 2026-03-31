package com.aero.dto;

import com.aero.domain.OperationStatus;

public record PlannedOperationSimpleDto(
        Long id,
        String autoNumber,
        String shortDescription,
        OperationStatus status,
        Integer routeKm
) {
}

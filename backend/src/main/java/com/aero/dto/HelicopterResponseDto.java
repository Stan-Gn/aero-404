package com.aero.dto;

import com.aero.domain.HelicopterStatus;

import java.time.LocalDate;

public record HelicopterResponseDto(
        Long id,
        String regNumber,
        String type,
        String description,
        Integer maxCrew,
        Integer maxPayload,
        HelicopterStatus status,
        LocalDate reviewDate,
        Integer rangeKm
) {
}

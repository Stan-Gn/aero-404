package com.aero.dto;

public record AirfieldResponseDto(
        Long id,
        String name,
        Double latitude,
        Double longitude
) {
}

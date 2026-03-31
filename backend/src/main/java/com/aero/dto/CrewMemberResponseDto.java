package com.aero.dto;

import com.aero.domain.CrewRole;

import java.time.LocalDate;

public record CrewMemberResponseDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        Integer weight,
        CrewRole role,
        String licenseNumber,
        LocalDate licenseExpiry,
        LocalDate trainingExpiry
) {
}

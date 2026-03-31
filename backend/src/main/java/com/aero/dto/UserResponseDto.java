package com.aero.dto;

import com.aero.domain.UserRole;

public record UserResponseDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        UserRole role
) {
}

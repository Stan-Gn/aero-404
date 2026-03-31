package com.aero.dto;

import com.aero.domain.UserRole;

public record RegisterResponseDto(
        Long id,
        String email,
        UserRole role
) {
}

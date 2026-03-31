package com.aero.dto;

import com.aero.domain.UserRole;

public record LoginResponseDto(
        String token,
        String email,
        UserRole role
) {
}

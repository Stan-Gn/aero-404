package com.aero.dto;

import com.aero.domain.UserRole;

public record UserSimpleDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        UserRole role
) {
}

package com.aero.dto;

import com.aero.domain.CrewRole;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CrewMemberRequestDto(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @NotNull(message = "Weight is required")
        @Min(value = 30, message = "Weight must be between 30 and 200 kg")
        @Max(value = 200, message = "Weight must be between 30 and 200 kg")
        Integer weight,

        @NotNull(message = "Role is required")
        CrewRole role,

        @Size(max = 30, message = "License number must be at most 30 characters")
        String licenseNumber,

        LocalDate licenseExpiry,

        @NotNull(message = "Training expiry date is required")
        LocalDate trainingExpiry
) {
}

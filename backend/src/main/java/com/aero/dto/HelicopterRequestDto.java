package com.aero.dto;

import com.aero.domain.HelicopterStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HelicopterRequestDto(
        @NotBlank(message = "Registration number is required")
        @Size(max = 30, message = "Registration number must be at most 30 characters")
        String regNumber,

        @NotBlank(message = "Type is required")
        @Size(max = 100, message = "Type must be at most 100 characters")
        String type,

        @Size(max = 100, message = "Description must be at most 100 characters")
        String description,

        @NotNull(message = "Max crew is required")
        @Min(value = 1, message = "Max crew must be between 1 and 10")
        @Max(value = 10, message = "Max crew must be between 1 and 10")
        Integer maxCrew,

        @NotNull(message = "Max payload is required")
        @Min(value = 1, message = "Max payload must be between 1 and 1000 kg")
        @Max(value = 1000, message = "Max payload must be between 1 and 1000 kg")
        Integer maxPayload,

        @NotNull(message = "Status is required")
        HelicopterStatus status,

        LocalDate reviewDate,

        @NotNull(message = "Range in km is required")
        @Min(value = 1, message = "Range must be between 1 and 1000 km")
        @Max(value = 1000, message = "Range must be between 1 and 1000 km")
        Integer rangeKm
) {
}

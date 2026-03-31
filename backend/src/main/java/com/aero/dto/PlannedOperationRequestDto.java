package com.aero.dto;

import com.aero.domain.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record PlannedOperationRequestDto(
        @NotBlank(message = "Order number is required")
        @Size(max = 100, message = "Order number must be at most 100 characters")
        String orderNumber,

        @NotBlank(message = "Short description is required")
        @Size(max = 200, message = "Short description must be at most 200 characters")
        String shortDescription,

        @NotNull(message = "Proposed date from is required")
        LocalDate proposedDateFrom,

        @NotNull(message = "Proposed date to is required")
        LocalDate proposedDateTo,

        @NotEmpty(message = "At least one activity type is required")
        Set<ActivityType> activityTypes,

        @Size(max = 500, message = "Additional info must be at most 500 characters")
        String additionalInfo,

        @Size(max = 500, message = "Contact emails must be at most 500 characters")
        String contactEmails
) {
}

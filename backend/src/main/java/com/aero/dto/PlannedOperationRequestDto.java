package com.aero.dto;

import com.aero.domain.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record PlannedOperationRequestDto(
        @NotBlank(message = "Order number is required")
        @Size(max = 30, message = "Order number must be at most 30 characters")
        String orderNumber,

        @NotBlank(message = "Short description is required")
        @Size(max = 100, message = "Short description must be at most 100 characters")
        String shortDescription,

        LocalDate proposedDateFrom,

        LocalDate proposedDateTo,

        @NotEmpty(message = "At least one activity type is required")
        Set<ActivityType> activityTypes,

        @Size(max = 500, message = "Additional info must be at most 500 characters")
        String additionalInfo,

        @Size(max = 500, message = "Contact emails must be at most 500 characters")
        String contactEmails
) {
}

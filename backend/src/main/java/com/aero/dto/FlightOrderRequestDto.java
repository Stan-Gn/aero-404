package com.aero.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;

public record FlightOrderRequestDto(
        @NotNull(message = "Planned departure is required")
        LocalDateTime plannedDeparture,

        @NotNull(message = "Planned landing is required")
        LocalDateTime plannedLanding,

        Long pilotId,

        @NotNull(message = "Helicopter is required")
        Long helicopterId,

        Set<Long> crewMemberIds,

        @NotNull(message = "Departure airfield is required")
        Long departureAirfieldId,

        @NotNull(message = "Arrival airfield is required")
        Long arrivalAirfieldId,

        @NotEmpty(message = "At least one planned operation is required")
        Set<Long> operationIds,

        @NotNull(message = "Estimated route km is required")
        @Min(value = 1, message = "Estimated route km must be at least 1")
        Integer estimatedRouteKm,

        LocalDateTime actualDeparture,

        LocalDateTime actualLanding
) {
}

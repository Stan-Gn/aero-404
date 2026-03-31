package com.aero.dto;

import com.aero.domain.FlightOrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record FlightOrderResponseDto(
        Long id,
        String autoNumber,
        LocalDateTime plannedDeparture,
        LocalDateTime plannedLanding,
        LocalDateTime actualDeparture,
        LocalDateTime actualLanding,
        CrewMemberResponseDto pilot,
        HelicopterResponseDto helicopter,
        List<CrewMemberResponseDto> crewMembers,
        AirfieldResponseDto departureAirfield,
        AirfieldResponseDto arrivalAirfield,
        List<PlannedOperationSimpleDto> plannedOperations,
        Integer crewWeight,
        Integer estimatedRouteKm,
        FlightOrderStatus status
) {
}

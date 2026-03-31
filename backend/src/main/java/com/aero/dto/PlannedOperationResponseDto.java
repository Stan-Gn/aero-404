package com.aero.dto;

import com.aero.domain.ActivityType;
import com.aero.domain.OperationStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record PlannedOperationResponseDto(
        Long id,
        String autoNumber,
        String orderNumber,
        String shortDescription,
        String kmlFileName,
        Integer routeKm,
        String routePoints,
        LocalDate proposedDateFrom,
        LocalDate proposedDateTo,
        LocalDate plannedDateFrom,
        LocalDate plannedDateTo,
        Set<ActivityType> activityTypes,
        String additionalInfo,
        OperationStatus status,
        UserSimpleDto createdBy,
        String contactEmails,
        String remarksAfterExecution,
        List<OperationCommentDto> comments,
        List<OperationHistoryDto> history
) {
}

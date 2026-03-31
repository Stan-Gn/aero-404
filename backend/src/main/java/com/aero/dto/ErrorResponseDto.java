package com.aero.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponseDto(
        LocalDateTime timestamp,
        int status,
        String message,
        List<String> errors
) {
    public static ErrorResponseDto of(int status, String message) {
        return new ErrorResponseDto(LocalDateTime.now(), status, message, List.of());
    }

    public static ErrorResponseDto of(int status, String message, List<String> errors) {
        return new ErrorResponseDto(LocalDateTime.now(), status, message, errors);
    }
}

package com.aero.controller;

import com.aero.domain.OperationStatus;
import com.aero.dto.OperationCommentDto;
import com.aero.dto.PlannedOperationRequestDto;
import com.aero.dto.PlannedOperationResponseDto;
import com.aero.service.PlannedOperationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/operations")
public class PlannedOperationController {

    private static final Logger log = LoggerFactory.getLogger(PlannedOperationController.class);

    @Autowired
    private PlannedOperationService operationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PLANNER','SUPERVISOR','PILOT')")
    public ResponseEntity<List<PlannedOperationResponseDto>> getAll(
            @RequestParam(required = false) OperationStatus status) {
        log.info("GET /api/v1/operations with status filter: {}", status);
        List<PlannedOperationResponseDto> operations = status != null
                ? operationService.findAll(status)
                : operationService.findAll();
        return ResponseEntity.ok(operations);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PLANNER','SUPERVISOR','PILOT')")
    public ResponseEntity<PlannedOperationResponseDto> getById(@PathVariable Long id) {
        log.info("GET /api/v1/operations/{}", id);
        PlannedOperationResponseDto operation = operationService.findById(id);
        return ResponseEntity.ok(operation);
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public ResponseEntity<PlannedOperationResponseDto> create(
            @Valid @RequestPart PlannedOperationRequestDto dto,
            @RequestPart(required = false) MultipartFile kmlFile) {
        log.info("POST /api/v1/operations - creating operation: {}", dto.orderNumber());

        String kmlContent = null;
        if (kmlFile != null && !kmlFile.isEmpty()) {
            try {
                kmlContent = new String(kmlFile.getBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("Failed to read KML file", e);
                return ResponseEntity.badRequest().build();
            }
        }

        PlannedOperationResponseDto created = operationService.create(dto, kmlContent);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public ResponseEntity<PlannedOperationResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestPart PlannedOperationRequestDto dto,
            @RequestPart(required = false) MultipartFile kmlFile) {
        log.info("PUT /api/v1/operations/{} - updating operation", id);

        String kmlContent = null;
        if (kmlFile != null && !kmlFile.isEmpty()) {
            try {
                kmlContent = new String(kmlFile.getBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("Failed to read KML file", e);
                return ResponseEntity.badRequest().build();
            }
        }

        PlannedOperationResponseDto updated = operationService.update(id, dto, kmlContent);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/v1/operations/{}", id);
        operationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<PlannedOperationResponseDto> reject(@PathVariable Long id) {
        log.info("POST /api/v1/operations/{}/reject", id);
        PlannedOperationResponseDto rejected = operationService.reject(id);
        return ResponseEntity.ok(rejected);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<PlannedOperationResponseDto> confirm(
            @PathVariable Long id,
            @RequestParam LocalDate plannedDateFrom,
            @RequestParam LocalDate plannedDateTo) {
        log.info("POST /api/v1/operations/{}/confirm", id);
        PlannedOperationResponseDto confirmed = operationService.confirmToPlan(id, plannedDateFrom, plannedDateTo);
        return ResponseEntity.ok(confirmed);
    }

    @PostMapping("/{id}/resign")
    @PreAuthorize("hasRole('PLANNER')")
    public ResponseEntity<PlannedOperationResponseDto> resign(@PathVariable Long id) {
        log.info("POST /api/v1/operations/{}/resign", id);
        PlannedOperationResponseDto resigned = operationService.resign(id);
        return ResponseEntity.ok(resigned);
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('ADMIN','PLANNER','SUPERVISOR','PILOT')")
    public ResponseEntity<OperationCommentDto> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request) {
        log.info("POST /api/v1/operations/{}/comments", id);
        OperationCommentDto comment = operationService.addComment(id, request.text());
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    record CommentRequest(
            @NotBlank(message = "Comment text is required")
            @Size(max = 500, message = "Comment must be at most 500 characters")
            String text
    ) {
    }
}

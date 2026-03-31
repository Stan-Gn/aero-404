package com.aero.controller;

import com.aero.domain.FlightOrderStatus;
import com.aero.dto.FlightOrderRequestDto;
import com.aero.dto.FlightOrderResponseDto;
import com.aero.service.FlightOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flight-orders")
public class FlightOrderController {

    private static final Logger log = LoggerFactory.getLogger(FlightOrderController.class);

    @Autowired
    private FlightOrderService flightOrderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PILOT','SUPERVISOR')")
    public ResponseEntity<List<FlightOrderResponseDto>> getAll(
            @RequestParam(required = false) FlightOrderStatus status) {
        log.info("GET /api/v1/flight-orders with status filter: {}", status);
        List<FlightOrderResponseDto> orders = status != null
                ? flightOrderService.findAll(status)
                : flightOrderService.findAll();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PILOT','SUPERVISOR')")
    public ResponseEntity<FlightOrderResponseDto> getById(@PathVariable Long id) {
        log.info("GET /api/v1/flight-orders/{}", id);
        FlightOrderResponseDto order = flightOrderService.findById(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping
    @PreAuthorize("hasRole('PILOT')")
    public ResponseEntity<FlightOrderResponseDto> create(@Valid @RequestBody FlightOrderRequestDto dto) {
        log.info("POST /api/v1/flight-orders - creating flight order");
        FlightOrderResponseDto created = flightOrderService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PILOT','SUPERVISOR')")
    public ResponseEntity<FlightOrderResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody FlightOrderRequestDto dto) {
        log.info("PUT /api/v1/flight-orders/{} - updating flight order", id);
        FlightOrderResponseDto updated = flightOrderService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('PILOT')")
    public ResponseEntity<FlightOrderResponseDto> submit(@PathVariable Long id) {
        log.info("POST /api/v1/flight-orders/{}/submit", id);
        FlightOrderResponseDto submitted = flightOrderService.submit(id);
        return ResponseEntity.ok(submitted);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<FlightOrderResponseDto> reject(@PathVariable Long id) {
        log.info("POST /api/v1/flight-orders/{}/reject", id);
        FlightOrderResponseDto rejected = flightOrderService.reject(id);
        return ResponseEntity.ok(rejected);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<FlightOrderResponseDto> accept(@PathVariable Long id) {
        log.info("POST /api/v1/flight-orders/{}/accept", id);
        FlightOrderResponseDto accepted = flightOrderService.accept(id);
        return ResponseEntity.ok(accepted);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('PILOT')")
    public ResponseEntity<FlightOrderResponseDto> complete(
            @PathVariable Long id,
            @Valid @RequestBody FlightOrderCompleteRequest request) {
        log.info("POST /api/v1/flight-orders/{}/complete with result: {}", id, request.result());
        FlightOrderResponseDto result = switch (request.result()) {
            case "DONE" -> flightOrderService.markDone(id);
            case "PARTIALLY_DONE" -> flightOrderService.markPartiallyDone(id);
            case "NOT_DONE" -> flightOrderService.markNotDone(id);
            default -> throw new IllegalArgumentException("Invalid result: " + request.result() + ". Must be DONE, PARTIALLY_DONE, or NOT_DONE");
        };
        return ResponseEntity.ok(result);
    }

    record FlightOrderCompleteRequest(
            @NotBlank(message = "Result is required")
            String result
    ) {
    }
}

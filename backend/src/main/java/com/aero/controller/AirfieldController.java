package com.aero.controller;

import com.aero.dto.AirfieldRequestDto;
import com.aero.dto.AirfieldResponseDto;
import com.aero.service.AirfieldService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/airfields")
public class AirfieldController {

    @Autowired
    private AirfieldService airfieldService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','PILOT')")
    public ResponseEntity<List<AirfieldResponseDto>> getAll() {
        return ResponseEntity.ok(airfieldService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','PILOT')")
    public ResponseEntity<AirfieldResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(airfieldService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirfieldResponseDto> create(@Valid @RequestBody AirfieldRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(airfieldService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirfieldResponseDto> update(@PathVariable Long id, @Valid @RequestBody AirfieldRequestDto dto) {
        return ResponseEntity.ok(airfieldService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        airfieldService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

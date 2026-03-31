package com.aero.controller;

import com.aero.dto.HelicopterRequestDto;
import com.aero.dto.HelicopterResponseDto;
import com.aero.service.HelicopterService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/helicopters")
public class HelicopterController {

    @Autowired
    private HelicopterService helicopterService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','PILOT')")
    public ResponseEntity<List<HelicopterResponseDto>> getAll() {
        return ResponseEntity.ok(helicopterService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','PILOT')")
    public ResponseEntity<HelicopterResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(helicopterService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HelicopterResponseDto> create(@Valid @RequestBody HelicopterRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(helicopterService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HelicopterResponseDto> update(@PathVariable Long id, @Valid @RequestBody HelicopterRequestDto dto) {
        return ResponseEntity.ok(helicopterService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        helicopterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

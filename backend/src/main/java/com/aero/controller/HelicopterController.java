package com.aero.controller;

import com.aero.dto.HelicopterRequestDto;
import com.aero.dto.HelicopterResponseDto;
import com.aero.service.HelicopterService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/helicopters")
public class HelicopterController {

    @Autowired
    private HelicopterService helicopterService;

    @GetMapping
    public ResponseEntity<List<HelicopterResponseDto>> getAll() {
        return ResponseEntity.ok(helicopterService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HelicopterResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(helicopterService.findById(id));
    }

    @PostMapping
    public ResponseEntity<HelicopterResponseDto> create(@Valid @RequestBody HelicopterRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(helicopterService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HelicopterResponseDto> update(@PathVariable Long id, @Valid @RequestBody HelicopterRequestDto dto) {
        return ResponseEntity.ok(helicopterService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        helicopterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

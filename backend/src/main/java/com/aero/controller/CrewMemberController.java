package com.aero.controller;

import com.aero.dto.CrewMemberRequestDto;
import com.aero.dto.CrewMemberResponseDto;
import com.aero.service.CrewMemberService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crew-members")
public class CrewMemberController {

    @Autowired
    private CrewMemberService crewMemberService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','PILOT')")
    public ResponseEntity<List<CrewMemberResponseDto>> getAll() {
        return ResponseEntity.ok(crewMemberService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','PILOT')")
    public ResponseEntity<CrewMemberResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(crewMemberService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CrewMemberResponseDto> create(@Valid @RequestBody CrewMemberRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crewMemberService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CrewMemberResponseDto> update(@PathVariable Long id, @Valid @RequestBody CrewMemberRequestDto dto) {
        return ResponseEntity.ok(crewMemberService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        crewMemberService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

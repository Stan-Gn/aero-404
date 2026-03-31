package com.aero.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "crew_members")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrewMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private Integer weight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrewRole role;

    @Column(length = 30)
    private String licenseNumber;

    private LocalDate licenseExpiry;

    @Column(nullable = false)
    private LocalDate trainingExpiry;
}

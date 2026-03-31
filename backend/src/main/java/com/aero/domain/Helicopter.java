package com.aero.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "helicopters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Helicopter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String regNumber;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(length = 100)
    private String description;

    @Column(nullable = false)
    private Integer maxCrew;

    @Column(nullable = false)
    private Integer maxPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HelicopterStatus status;

    private LocalDate reviewDate;

    @Column(nullable = false)
    private Integer rangeKm;
}

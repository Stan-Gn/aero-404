package com.aero.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "planned_operations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannedOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String autoNumber;

    @Column(length = 30)
    private String orderNumber;

    @Column(nullable = false, length = 100)
    private String shortDescription;

    @Column(length = 255)
    private String kmlFileName;

    @Lob
    private String kmlContent;

    private Integer routeKm;

    @Column(columnDefinition = "TEXT")
    private String routePoints;

    private LocalDate proposedDateFrom;

    private LocalDate proposedDateTo;

    private LocalDate plannedDateFrom;

    private LocalDate plannedDateTo;

    @ElementCollection(targetClass = ActivityType.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "operation_activity_types", joinColumns = @JoinColumn(name = "operation_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private Set<ActivityType> activityTypes;

    @Column(columnDefinition = "TEXT")
    private String additionalInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private AppUser createdBy;

    @Column(length = 500)
    private String contactEmails;

    @Column(columnDefinition = "TEXT")
    private String remarksAfterExecution;
}

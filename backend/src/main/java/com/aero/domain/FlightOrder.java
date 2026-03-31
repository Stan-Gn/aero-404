package com.aero.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "flight_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String autoNumber;

    @Column(nullable = false)
    private LocalDateTime plannedDeparture;

    @Column(nullable = false)
    private LocalDateTime plannedLanding;

    private LocalDateTime actualDeparture;

    private LocalDateTime actualLanding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pilot_id", nullable = false)
    private CrewMember pilot;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "flight_order_crew_members",
            joinColumns = @JoinColumn(name = "flight_order_id"),
            inverseJoinColumns = @JoinColumn(name = "crew_member_id")
    )
    private Set<CrewMember> crewMembers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "helicopter_id", nullable = false)
    private Helicopter helicopter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_airfield_id", nullable = false)
    private Airfield departureAirfield;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_airfield_id", nullable = false)
    private Airfield arrivalAirfield;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "flight_order_operations",
            joinColumns = @JoinColumn(name = "flight_order_id"),
            inverseJoinColumns = @JoinColumn(name = "operation_id")
    )
    private Set<PlannedOperation> plannedOperations;

    private Integer crewWeight;

    @Column(nullable = false)
    private Integer estimatedRouteKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlightOrderStatus status;
}

package com.aero.service;

import com.aero.domain.*;
import com.aero.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testy logiki walidacji zlecenia na lot — FlightOrderValidationService.
 * Pokrywają 5 warunków blokujących zapis (PRD 6.6.c):
 *   1. Przegląd helikoptera
 *   2. Licencja pilota
 *   3. Szkolenia załogi
 *   4. Waga załogi
 *   5. Zasięg trasy
 *
 * Brak Spring contextu — czysty unit test z Mockito (brak Mockito — encje tworzone ręcznie).
 */
@DisplayName("FlightOrderValidationService")
class FlightOrderValidationServiceTest {

    private FlightOrderValidationService validationService;
    private LocalDate flightDate;  // Jutro — punkt odniesienia dla wszystkich testów

    @BeforeEach
    void setUp() {
        validationService = new FlightOrderValidationService();
        flightDate = LocalDate.now().plusDays(1);
    }

    // ===== Pomocnicze metody do budowy encji =====

    private Helicopter baseHelicopter() {
        return Helicopter.builder()
                .id(1L)
                .regNumber("SP-TEST")
                .type("H125")
                .description("Test helicopter")
                .maxCrew(3)
                .maxPayload(500)
                .status(HelicopterStatus.ACTIVE)
                .reviewDate(flightDate.plusDays(30))  // Ważny do jutro + 30 dni
                .rangeKm(300)
                .build();
    }

    private CrewMember basePilot() {
        return CrewMember.builder()
                .id(1L)
                .firstName("John")
                .lastName("Pilot")
                .email("pilot@aero.com")
                .weight(75)
                .role(CrewRole.PILOT)
                .licenseNumber("LIC-001")
                .licenseExpiry(flightDate.plusDays(365))  // Licencja ważna rok
                .trainingExpiry(flightDate.plusDays(180))  // Szkolenie ważne 6 miesięcy
                .build();
    }

    private CrewMember baseObserver() {
        return CrewMember.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Observer")
                .email("observer@aero.com")
                .weight(65)
                .role(CrewRole.OBSERVER)
                .licenseNumber(null)
                .licenseExpiry(null)
                .trainingExpiry(flightDate.plusDays(180))
                .build();
    }

    private FlightOrder baseOrder() {
        return FlightOrder.builder()
                .id(1L)
                .autoNumber("FO-2026-00001")
                .plannedDeparture(LocalDateTime.of(flightDate, java.time.LocalTime.of(10, 0)))
                .plannedLanding(LocalDateTime.of(flightDate, java.time.LocalTime.of(12, 0)))
                .pilot(basePilot())
                .crewMembers(new HashSet<>())  // Pusta lista — bez dodatkowych członków
                .helicopter(baseHelicopter())
                .departureAirfield(null)  // Nie używane w walidacji
                .arrivalAirfield(null)
                .plannedOperations(null)
                .crewWeight(150)  // 75 (pilot) + 75 (dodatkowy, jeśli by był)
                .estimatedRouteKm(200)  // Poniżej zasięgu 300 km
                .status(FlightOrderStatus.INTRODUCED)
                .build();
    }

    // ===== HAPPY PATH =====

    @Nested
    @DisplayName("Happy path — wszystkie warunki spełnione")
    class HappyPath {

        @Test
        @DisplayName("Wszystkie warunki prawidłowe — brak wyjątku")
        void allConditionsMet_doesNotThrow() {
            FlightOrder order = baseOrder();
            assertDoesNotThrow(() -> validationService.validate(order));
        }
    }

    // ===== WALIDACJA PRZEGLĄDU HELIKOPTERA =====

    @Nested
    @DisplayName("Przegląd helikoptera (reviewDate)")
    class HelicopterReviewValidation {

        @Test
        @DisplayName("reviewDate = null → HelicopterReviewExpiredException")
        void reviewDateNull_throws() {
            FlightOrder order = baseOrder();
            order.getHelicopter().setReviewDate(null);

            assertThrows(HelicopterReviewExpiredException.class,
                    () -> validationService.validate(order));
        }

        @Test
        @DisplayName("reviewDate < flightDate → HelicopterReviewExpiredException")
        void reviewDateBeforeFlightDate_throws() {
            FlightOrder order = baseOrder();
            order.getHelicopter().setReviewDate(flightDate.minusDays(1));

            assertThrows(HelicopterReviewExpiredException.class,
                    () -> validationService.validate(order));
        }

        @Test
        @DisplayName("reviewDate == flightDate → brak wyjątku (isBefore = false)")
        void reviewDateEqualsFlightDate_doesNotThrow() {
            FlightOrder order = baseOrder();
            order.getHelicopter().setReviewDate(flightDate);

            assertDoesNotThrow(() -> validationService.validate(order));
        }

        @Test
        @DisplayName("reviewDate > flightDate → brak wyjątku")
        void reviewDateAfterFlightDate_doesNotThrow() {
            FlightOrder order = baseOrder();
            order.getHelicopter().setReviewDate(flightDate.plusDays(100));

            assertDoesNotThrow(() -> validationService.validate(order));
        }
    }

    // ===== WALIDACJA LICENCJI PILOTA =====

    @Nested
    @DisplayName("Licencja pilota (licenseExpiry)")
    class PilotLicenseValidation {

        @Test
        @DisplayName("licenseExpiry = null → PilotLicenseExpiredException")
        void licenseExpiryNull_throws() {
            FlightOrder order = baseOrder();
            order.getPilot().setLicenseExpiry(null);

            assertThrows(PilotLicenseExpiredException.class,
                    () -> validationService.validate(order));
        }

        @Test
        @DisplayName("licenseExpiry < flightDate → PilotLicenseExpiredException")
        void licenseExpiryBeforeFlightDate_throws() {
            FlightOrder order = baseOrder();
            order.getPilot().setLicenseExpiry(flightDate.minusDays(1));

            assertThrows(PilotLicenseExpiredException.class,
                    () -> validationService.validate(order));
        }

        @Test
        @DisplayName("licenseExpiry == flightDate → brak wyjątku (isBefore = false)")
        void licenseExpiryEqualsFlightDate_doesNotThrow() {
            FlightOrder order = baseOrder();
            order.getPilot().setLicenseExpiry(flightDate);

            assertDoesNotThrow(() -> validationService.validate(order));
        }

        @Test
        @DisplayName("licenseExpiry > flightDate → brak wyjątku")
        void licenseExpiryAfterFlightDate_doesNotThrow() {
            FlightOrder order = baseOrder();
            order.getPilot().setLicenseExpiry(flightDate.plusDays(365));

            assertDoesNotThrow(() -> validationService.validate(order));
        }
    }

    // ===== WALIDACJA SZKOLEŃ ZAŁOGI =====

    @Nested
    @DisplayName("Szkolenie załogi (trainingExpiry)")
    class CrewTrainingValidation {

        @Test
        @DisplayName("Pilot: trainingExpiry = null → CrewMemberTrainingExpiredException")
        void pilotTrainingNull_throws() {
            FlightOrder order = baseOrder();
            order.getPilot().setTrainingExpiry(null);

            assertThrows(CrewMemberTrainingExpiredException.class,
                    () -> validationService.validate(order));
        }

        @Test
        @DisplayName("Pilot: trainingExpiry < flightDate → CrewMemberTrainingExpiredException")
        void pilotTrainingBeforeFlightDate_throws() {
            FlightOrder order = baseOrder();
            order.getPilot().setTrainingExpiry(flightDate.minusDays(1));

            assertThrows(CrewMemberTrainingExpiredException.class,
                    () -> validationService.validate(order));
        }

        @Test
        @DisplayName("Pilot: trainingExpiry == flightDate → brak wyjątku")
        void pilotTrainingEqualsFlightDate_doesNotThrow() {
            FlightOrder order = baseOrder();
            order.getPilot().setTrainingExpiry(flightDate);

            assertDoesNotThrow(() -> validationService.validate(order));
        }

        @Test
        @DisplayName("Crew member: trainingExpiry < flightDate → CrewMemberTrainingExpiredException")
        void crewMemberTrainingExpired_throws() {
            FlightOrder order = baseOrder();
            CrewMember observer = baseObserver();
            observer.setTrainingExpiry(flightDate.minusDays(1));
            order.setCrewMembers(Set.of(observer));

            assertThrows(CrewMemberTrainingExpiredException.class,
                    () -> validationService.validate(order));
        }

        @Test
        @DisplayName("crewMembers = null → brak wyjątku (null pomijany)")
        void nullCrewMembersSet_doesNotThrow() {
            FlightOrder order = baseOrder();
            order.setCrewMembers(null);

            assertDoesNotThrow(() -> validationService.validate(order));
        }

        @Test
        @DisplayName("crewMembers = Set.of() → brak wyjątku (pusty set)")
        void emptyCrewMembersSet_doesNotThrow() {
            FlightOrder order = baseOrder();
            order.setCrewMembers(new HashSet<>());

            assertDoesNotThrow(() -> validationService.validate(order));
        }

        @Test
        @DisplayName("Wielu crew members z ważnym szkoleniem → brak wyjątku")
        void allCrewMembersValid_doesNotThrow() {
            FlightOrder order = baseOrder();
            CrewMember observer1 = baseObserver();
            observer1.setId(2L);
            observer1.setEmail("obs1@aero.com");

            CrewMember observer2 = baseObserver();
            observer2.setId(3L);
            observer2.setEmail("obs2@aero.com");
            observer2.setTrainingExpiry(flightDate.plusDays(365));

            order.setCrewMembers(Set.of(observer1, observer2));

            assertDoesNotThrow(() -> validationService.validate(order));
        }
    }

    // ===== WALIDACJA WAGI ZAŁOGI =====

    @Nested
    @DisplayName("Waga załogi (crewWeight)")
    class CrewWeightValidation {

        @Test
        @DisplayName("crewWeight = null → brak wyjątku (null pomijany)")
        void crewWeightNull_doesNotThrow() {
            FlightOrder order = baseOrder();
            order.setCrewWeight(null);

            assertDoesNotThrow(() -> validationService.validate(order));
        }

        @Test
        @DisplayName("crewWeight == maxPayload → brak wyjątku (> nie >=)")
        void crewWeightEqualsMaxPayload_doesNotThrow() {
            FlightOrder order = baseOrder();
            order.setCrewWeight(500);
            order.getHelicopter().setMaxPayload(500);

            assertDoesNotThrow(() -> validationService.validate(order));
        }

        @Test
        @DisplayName("crewWeight > maxPayload → CrewWeightExceededException")
        void crewWeightExceedsMaxPayload_throws() {
            FlightOrder order = baseOrder();
            order.setCrewWeight(501);
            order.getHelicopter().setMaxPayload(500);

            assertThrows(CrewWeightExceededException.class,
                    () -> validationService.validate(order));
        }

        @Test
        @DisplayName("crewWeight < maxPayload → brak wyjątku")
        void crewWeightBelowMaxPayload_doesNotThrow() {
            FlightOrder order = baseOrder();
            order.setCrewWeight(300);
            order.getHelicopter().setMaxPayload(500);

            assertDoesNotThrow(() -> validationService.validate(order));
        }
    }

    // ===== WALIDACJA ZASIĘGU TRASY =====

    @Nested
    @DisplayName("Zasięg trasy (estimatedRouteKm)")
    class RouteRangeValidation {

        @Test
        @DisplayName("estimatedRouteKm = null → brak wyjątku (null pomijany)")
        void estimatedRouteKmNull_doesNotThrow() {
            FlightOrder order = baseOrder();
            order.setEstimatedRouteKm(null);

            assertDoesNotThrow(() -> validationService.validate(order));
        }

        @Test
        @DisplayName("estimatedRouteKm == rangeKm → brak wyjątku (> nie >=)")
        void estimatedRouteKmEqualsRangeKm_doesNotThrow() {
            FlightOrder order = baseOrder();
            order.setEstimatedRouteKm(300);
            order.getHelicopter().setRangeKm(300);

            assertDoesNotThrow(() -> validationService.validate(order));
        }

        @Test
        @DisplayName("estimatedRouteKm > rangeKm → RouteExceedsRangeException")
        void estimatedRouteKmExceedsRangeKm_throws() {
            FlightOrder order = baseOrder();
            order.setEstimatedRouteKm(301);
            order.getHelicopter().setRangeKm(300);

            assertThrows(RouteExceedsRangeException.class,
                    () -> validationService.validate(order));
        }

        @Test
        @DisplayName("estimatedRouteKm < rangeKm → brak wyjątku")
        void estimatedRouteKmBelowRangeKm_doesNotThrow() {
            FlightOrder order = baseOrder();
            order.setEstimatedRouteKm(200);
            order.getHelicopter().setRangeKm(300);

            assertDoesNotThrow(() -> validationService.validate(order));
        }
    }
}

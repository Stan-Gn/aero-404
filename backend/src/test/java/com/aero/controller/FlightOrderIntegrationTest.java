package com.aero.controller;

import com.aero.domain.*;
import com.aero.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testy integracyjne FlightOrderController z bazą H2 (in-memory).
 *
 * Uruchamiają pełny kontekst Spring Boot z profilem "test" (H2 zamiast PostgreSQL).
 * Każdy test owinięty w transakcję → automatyczny rollback po teście.
 *
 * Scenariusze:
 *  - GET lista / GET by id
 *  - Tworzenie zlecenia (happy path, walidacje)
 *  - Przejścia statusów: INTRODUCED→SUBMITTED→ACCEPTED→DONE
 *  - Odrzucenie: INTRODUCED→SUBMITTED→REJECTED
 *  - Blokada roli PLANNER
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FlightOrderIntegrationTest {

    private static final String BASE_URL = "/api/v1/flight-orders";
    private static final String PILOT_EMAIL = "it-pilot@test.pl";

    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired HelicopterRepository helicopterRepository;
    @Autowired CrewMemberRepository crewMemberRepository;
    @Autowired AirfieldRepository airfieldRepository;
    @Autowired PlannedOperationRepository operationRepository;
    @Autowired FlightOrderRepository flightOrderRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @PersistenceContext EntityManager entityManager;

    private Long helicopterId;
    private Long pilotCrewMemberId;
    private Long departureId;
    private Long arrivalId;
    private Long operationId;

    @BeforeEach
    void setUp() {
        AppUser pilotUser = userRepository.save(AppUser.builder()
                .firstName("Jan").lastName("Pilotowski")
                .email(PILOT_EMAIL)
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.PILOT)
                .build());

        Helicopter helicopter = helicopterRepository.save(Helicopter.builder()
                .regNumber("IT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .type("EC135").description("Helikopter testowy")
                .maxCrew(4).maxPayload(600)
                .status(HelicopterStatus.ACTIVE)
                .reviewDate(LocalDate.of(2028, 12, 31))
                .rangeKm(700)
                .build());
        helicopterId = helicopter.getId();

        CrewMember pilot = crewMemberRepository.save(CrewMember.builder()
                .firstName("Jan").lastName("Pilotowski")
                .email(PILOT_EMAIL)
                .weight(80).role(CrewRole.PILOT)
                .licenseNumber("LIC-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .licenseExpiry(LocalDate.of(2028, 12, 31))
                .trainingExpiry(LocalDate.of(2028, 12, 31))
                .build());
        pilotCrewMemberId = pilot.getId();

        Airfield dep = airfieldRepository.save(Airfield.builder()
                .name("WAW-IT").latitude(52.2297).longitude(21.0122).build());
        departureId = dep.getId();

        Airfield arr = airfieldRepository.save(Airfield.builder()
                .name("KRK-IT").latitude(50.0777).longitude(19.7848).build());
        arrivalId = arr.getId();

        PlannedOperation op = operationRepository.save(PlannedOperation.builder()
                .autoNumber("OP-IT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .shortDescription("Operacja integracyjna")
                .status(OperationStatus.CONFIRMED)
                .createdBy(pilotUser)
                .activityTypes(Set.of(ActivityType.PATROL))
                .build());
        operationId = op.getId();

        // flush do H2 + wyczyść L1 cache, żeby serwis czytał świeże dane z DB
        entityManager.flush();
        entityManager.clear();
    }

    // --- Helpers ---

    private String validJson() {
        return """
                {
                    "plannedDeparture": "2027-06-15T08:00:00",
                    "plannedLanding":   "2027-06-15T12:00:00",
                    "pilotId":           %d,
                    "helicopterId":      %d,
                    "crewMemberIds":     [],
                    "departureAirfieldId": %d,
                    "arrivalAirfieldId":   %d,
                    "operationIds":      [%d],
                    "estimatedRouteKm":  150
                }
                """.formatted(pilotCrewMemberId, helicopterId, departureId, arrivalId, operationId);
    }

    /** Tworzy zlecenie jako PILOT i zwraca jego id. */
    private Long createOrder() throws Exception {
        String body = mockMvc.perform(post(BASE_URL)
                        .with(user(PILOT_EMAIL).roles("PILOT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        entityManager.flush();
        entityManager.clear();
        return new ObjectMapper().readTree(body).get("id").asLong();
    }

    // ==================== GET ====================

    @Test
    @DisplayName("GET /flight-orders — 200, zwraca tablicę")
    void getAll_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL).with(user(PILOT_EMAIL).roles("PILOT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /flight-orders — PLANNER → 403")
    void getAll_asPlanner_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL).with(user("planner@test.pl").roles("PLANNER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /flight-orders/{id} — nie istnieje → 404")
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").with(user(PILOT_EMAIL).roles("PILOT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("FlightOrder with id 999999 not found"));
    }

    @Test
    @DisplayName("GET /flight-orders/{id} — po create → 200 z pełnymi danymi")
    void getById_afterCreate_returns200() throws Exception {
        Long id = createOrder();

        mockMvc.perform(get(BASE_URL + "/" + id).with(user(PILOT_EMAIL).roles("PILOT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("INTRODUCED"))
                .andExpect(jsonPath("$.pilot.email").value(PILOT_EMAIL))
                .andExpect(jsonPath("$.helicopter.status").value("ACTIVE"))
                .andExpect(jsonPath("$.plannedOperations", hasSize(1)))
                .andExpect(jsonPath("$.estimatedRouteKm").value(150))
                .andExpect(jsonPath("$.crewWeight").value(80));
    }

    // ==================== CREATE ====================

    @Test
    @DisplayName("POST /flight-orders — poprawne dane → 201, status INTRODUCED")
    void create_validDto_returns201() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user(PILOT_EMAIL).roles("PILOT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.autoNumber", startsWith("FO-")))
                .andExpect(jsonPath("$.status").value("INTRODUCED"))
                .andExpect(jsonPath("$.estimatedRouteKm").value(150))
                .andExpect(jsonPath("$.pilot.email").value(PILOT_EMAIL))
                .andExpect(jsonPath("$.crewWeight").value(80));
    }

    @Test
    @DisplayName("POST /flight-orders — create zmienia status operacji na SCHEDULED")
    void create_changesOperationStatusToScheduled() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user(PILOT_EMAIL).roles("PILOT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isCreated());

        entityManager.flush();
        entityManager.clear();

        PlannedOperation op = operationRepository.findById(operationId).orElseThrow();
        assertThat(op.getStatus()).isEqualTo(OperationStatus.SCHEDULED);
    }

    @Test
    @DisplayName("POST /flight-orders — helikopter INACTIVE → 400")
    void create_inactiveHelicopter_returns400() throws Exception {
        Helicopter inactive = helicopterRepository.save(Helicopter.builder()
                .regNumber("OFF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .type("R44").description("Nieaktywny").maxCrew(2).maxPayload(300)
                .status(HelicopterStatus.INACTIVE).rangeKm(300)
                .build());
        entityManager.flush();
        entityManager.clear();

        String json = """
                {"plannedDeparture":"2027-06-15T08:00:00","plannedLanding":"2027-06-15T12:00:00",
                 "pilotId":%d,"helicopterId":%d,"crewMemberIds":[],
                 "departureAirfieldId":%d,"arrivalAirfieldId":%d,"operationIds":[%d],"estimatedRouteKm":150}
                """.formatted(pilotCrewMemberId, inactive.getId(), departureId, arrivalId, operationId);

        mockMvc.perform(post(BASE_URL)
                        .with(user(PILOT_EMAIL).roles("PILOT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Helicopter must be in ACTIVE status"));
    }

    @Test
    @DisplayName("POST /flight-orders — operacja nie w CONFIRMED → 400")
    void create_operationNotConfirmed_returns400() throws Exception {
        PlannedOperation op = operationRepository.findById(operationId).orElseThrow();
        op.setStatus(OperationStatus.INTRODUCED);
        operationRepository.save(op);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post(BASE_URL)
                        .with(user(PILOT_EMAIL).roles("PILOT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must be in CONFIRMED status")));
    }

    @Test
    @DisplayName("POST /flight-orders — PLANNER → 403")
    void create_asPlanner_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user("planner@test.pl").roles("PLANNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /flight-orders — brak wymaganych pól → 400 z listą błędów")
    void create_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user(PILOT_EMAIL).roles("PILOT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(3))));
    }

    // ==================== SUBMIT ====================

    @Test
    @DisplayName("POST /flight-orders/{id}/submit — INTRODUCED → SUBMITTED")
    void submit_changesStatusToSubmitted() throws Exception {
        Long id = createOrder();

        mockMvc.perform(post(BASE_URL + "/" + id + "/submit")
                        .with(user(PILOT_EMAIL).roles("PILOT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("POST /flight-orders/{id}/submit — ponowny submit → 400")
    void submit_alreadySubmitted_returns400() throws Exception {
        Long id = createOrder();

        mockMvc.perform(post(BASE_URL + "/" + id + "/submit")
                .with(user(PILOT_EMAIL).roles("PILOT")));
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post(BASE_URL + "/" + id + "/submit")
                        .with(user(PILOT_EMAIL).roles("PILOT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Can only submit flight orders in INTRODUCED status"));
    }

    // ==================== REJECT ====================

    @Test
    @DisplayName("POST /flight-orders/{id}/reject — SUBMITTED → REJECTED")
    void reject_afterSubmit_returnsRejected() throws Exception {
        Long id = createOrder();

        mockMvc.perform(post(BASE_URL + "/" + id + "/submit")
                .with(user(PILOT_EMAIL).roles("PILOT")));
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post(BASE_URL + "/" + id + "/reject")
                        .with(user("supervisor@test.pl").roles("SUPERVISOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("POST /flight-orders/{id}/reject — złe status (INTRODUCED) → 400")
    void reject_wrongStatus_returns400() throws Exception {
        Long id = createOrder();

        mockMvc.perform(post(BASE_URL + "/" + id + "/reject")
                        .with(user("supervisor@test.pl").roles("SUPERVISOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Can only reject flight orders in SUBMITTED status"));
    }

    // ==================== ACCEPT ====================

    @Test
    @DisplayName("POST /flight-orders/{id}/accept — SUBMITTED → ACCEPTED")
    void accept_afterSubmit_returnsAccepted() throws Exception {
        Long id = createOrder();

        mockMvc.perform(post(BASE_URL + "/" + id + "/submit")
                .with(user(PILOT_EMAIL).roles("PILOT")));
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post(BASE_URL + "/" + id + "/accept")
                        .with(user("supervisor@test.pl").roles("SUPERVISOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    // ==================== PEŁNY CYKL ŻYCIA ====================

    @Test
    @DisplayName("Cykl życia: INTRODUCED → SUBMITTED → ACCEPTED → DONE, operacja → DONE")
    void lifecycle_createSubmitAcceptDone() throws Exception {
        Long id = createOrder();

        // INTRODUCED → SUBMITTED
        mockMvc.perform(post(BASE_URL + "/" + id + "/submit")
                .with(user(PILOT_EMAIL).roles("PILOT"))).andExpect(status().isOk());
        entityManager.flush(); entityManager.clear();

        // SUBMITTED → ACCEPTED
        mockMvc.perform(post(BASE_URL + "/" + id + "/accept")
                .with(user("supervisor@test.pl").roles("SUPERVISOR"))).andExpect(status().isOk());
        entityManager.flush(); entityManager.clear();

        // ACCEPTED → DONE
        mockMvc.perform(post(BASE_URL + "/" + id + "/complete")
                        .with(user(PILOT_EMAIL).roles("PILOT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"result": "Zrealizowane w całości"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        // weryfikacja statusu operacji
        entityManager.flush(); entityManager.clear();
        PlannedOperation op = operationRepository.findById(operationId).orElseThrow();
        assertThat(op.getStatus()).isEqualTo(OperationStatus.DONE);
    }

    @Test
    @DisplayName("Cykl życia: INTRODUCED → SUBMITTED → ACCEPTED → NOT_DONE, operacja → CONFIRMED")
    void lifecycle_createSubmitAcceptNotDone() throws Exception {
        Long id = createOrder();

        mockMvc.perform(post(BASE_URL + "/" + id + "/submit")
                .with(user(PILOT_EMAIL).roles("PILOT")));
        entityManager.flush(); entityManager.clear();

        mockMvc.perform(post(BASE_URL + "/" + id + "/accept")
                .with(user("supervisor@test.pl").roles("SUPERVISOR")));
        entityManager.flush(); entityManager.clear();

        mockMvc.perform(post(BASE_URL + "/" + id + "/complete")
                        .with(user(PILOT_EMAIL).roles("PILOT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"result": "Nie zrealizowane"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_DONE"));

        // operacja wraca do CONFIRMED
        entityManager.flush(); entityManager.clear();
        PlannedOperation op = operationRepository.findById(operationId).orElseThrow();
        assertThat(op.getStatus()).isEqualTo(OperationStatus.CONFIRMED);
    }

    // TODO: odkomentować gdy frontend doda pola actualDeparture/actualLanding
    // @Test
    // @DisplayName("POST /flight-orders/{id}/complete — brak actualDeparture/actualLanding → 400")
    // void complete_withoutActualTimes_returns400() throws Exception {
    //     Long id = createOrder();
    //
    //     mockMvc.perform(post(BASE_URL + "/" + id + "/submit")
    //             .with(user(PILOT_EMAIL).roles("PILOT")));
    //     entityManager.flush(); entityManager.clear();
    //
    //     mockMvc.perform(post(BASE_URL + "/" + id + "/accept")
    //             .with(user("supervisor@test.pl").roles("SUPERVISOR")));
    //     entityManager.flush(); entityManager.clear();
    //
    //     mockMvc.perform(post(BASE_URL + "/" + id + "/complete")
    //                     .with(user(PILOT_EMAIL).roles("PILOT"))
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content("""
    //                             {"result": "Zrealizowane w całości"}
    //                             """))
    //             .andExpect(status().isBadRequest())
    //             .andExpect(jsonPath("$.message", containsString("Actual departure and landing times are required")));
    // }
}

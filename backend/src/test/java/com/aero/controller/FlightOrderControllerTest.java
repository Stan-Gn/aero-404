package com.aero.controller;

import com.aero.config.SecConf;
import com.aero.domain.*;
import com.aero.dto.*;
import com.aero.exception.EntityNotFoundException;
import com.aero.exception.GlobalExceptionHandler;
import com.aero.exception.ValidationException;
import com.aero.exception.*;
import com.aero.security.JwtTokenProvider;
import com.aero.service.FlightOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FlightOrderController.class)
@Import({SecConf.class, GlobalExceptionHandler.class})
@WithMockUser(roles = "PILOT")
class FlightOrderControllerTest {

    private static final String BASE_URL = "/api/v1/flight-orders";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FlightOrderService flightOrderService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // --- Helpers ---

    private static final CrewMemberResponseDto SAMPLE_PILOT = new CrewMemberResponseDto(
            1L, "Jan", "Pilotowski", "pilot@test.pl", 80,
            CrewRole.PILOT, "LIC-001", LocalDate.of(2027, 12, 31), LocalDate.of(2027, 12, 31));

    private static final HelicopterResponseDto SAMPLE_HELICOPTER = new HelicopterResponseDto(
            1L, "SP-HEL1", "EC135", "Helikopter testowy", 4, 500,
            HelicopterStatus.ACTIVE, LocalDate.of(2027, 12, 31), 600);

    private static final AirfieldResponseDto SAMPLE_DEPARTURE = new AirfieldResponseDto(
            1L, "Lotnisko Warszawa", 52.2297, 21.0122);

    private static final AirfieldResponseDto SAMPLE_ARRIVAL = new AirfieldResponseDto(
            2L, "Lotnisko Kraków", 50.0777, 19.7848);

    private static final PlannedOperationSimpleDto SAMPLE_OPERATION = new PlannedOperationSimpleDto(
            1L, "OP-2026-ABCD1234", "Inspekcja mostu", OperationStatus.SCHEDULED, 150);

    private static FlightOrderResponseDto sampleResponse(Long id, FlightOrderStatus status) {
        return new FlightOrderResponseDto(
                id, "FO-2026-ABCD1234",
                LocalDateTime.of(2026, 6, 15, 8, 0),
                LocalDateTime.of(2026, 6, 15, 12, 0),
                null, null,
                SAMPLE_PILOT, SAMPLE_HELICOPTER,
                List.of(), SAMPLE_DEPARTURE, SAMPLE_ARRIVAL,
                List.of(SAMPLE_OPERATION),
                80, 150, status);
    }

    private static final String VALID_DTO_JSON = """
            {
                "plannedDeparture": "2026-06-15T08:00:00",
                "plannedLanding": "2026-06-15T12:00:00",
                "pilotId": 1,
                "helicopterId": 1,
                "crewMemberIds": [],
                "departureAirfieldId": 1,
                "arrivalAirfieldId": 2,
                "operationIds": [1],
                "estimatedRouteKm": 150
            }
            """;

    // ==================== GET ALL ====================

    @Nested
    @DisplayName("GET /api/v1/flight-orders")
    class GetAllTests {

        @Test
        @DisplayName("returns 200 with list")
        void getAll_returns200() throws Exception {
            when(flightOrderService.findAll()).thenReturn(List.of(
                    sampleResponse(1L, FlightOrderStatus.SUBMITTED)));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].autoNumber").value("FO-2026-ABCD1234"));
        }

        @Test
        @DisplayName("with status filter returns 200")
        void getAll_withStatusFilter_returns200() throws Exception {
            when(flightOrderService.findAll(FlightOrderStatus.INTRODUCED)).thenReturn(List.of(
                    sampleResponse(1L, FlightOrderStatus.INTRODUCED)));

            mockMvc.perform(get(BASE_URL).param("status", "INTRODUCED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("INTRODUCED"));
        }

        @Test
        @DisplayName("returns 200 with empty list")
        void getAll_empty_returns200() throws Exception {
            when(flightOrderService.findAll()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ==================== GET BY ID ====================

    @Nested
    @DisplayName("GET /api/v1/flight-orders/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("happy path — returns 200")
        void getById_returns200() throws Exception {
            when(flightOrderService.findById(1L)).thenReturn(sampleResponse(1L, FlightOrderStatus.INTRODUCED));

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.autoNumber").value("FO-2026-ABCD1234"))
                    .andExpect(jsonPath("$.status").value("INTRODUCED"))
                    .andExpect(jsonPath("$.estimatedRouteKm").value(150))
                    .andExpect(jsonPath("$.pilot.email").value("pilot@test.pl"))
                    .andExpect(jsonPath("$.helicopter.regNumber").value("SP-HEL1"));
        }

        @Test
        @DisplayName("not found — returns 404")
        void getById_notFound_returns404() throws Exception {
            when(flightOrderService.findById(999L))
                    .thenThrow(new EntityNotFoundException("FlightOrder", 999L));

            mockMvc.perform(get(BASE_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("FlightOrder with id 999 not found"));
        }
    }

    // ==================== CREATE ====================

    @Nested
    @DisplayName("POST /api/v1/flight-orders")
    class CreateTests {

        @Test
        @DisplayName("happy path — valid dto returns 201")
        void create_returns201() throws Exception {
            when(flightOrderService.create(any())).thenReturn(sampleResponse(1L, FlightOrderStatus.INTRODUCED));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_DTO_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("INTRODUCED"));
        }

        @Test
        @DisplayName("helicopter not active — service throws ValidationException → 400")
        void create_helicopterNotActive_returns400() throws Exception {
            when(flightOrderService.create(any()))
                    .thenThrow(new ValidationException("Helicopter must be in ACTIVE status"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_DTO_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Helicopter must be in ACTIVE status"));
        }

        @Test
        @DisplayName("operation not confirmed — service throws ValidationException → 400")
        void create_operationNotConfirmed_returns400() throws Exception {
            when(flightOrderService.create(any()))
                    .thenThrow(new ValidationException("Operation OP-2026-ABCD1234 must be in CONFIRMED status"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_DTO_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Operation OP-2026-ABCD1234 must be in CONFIRMED status"));
        }

        @Test
        @DisplayName("helicopter review expired — returns 400")
        void create_helicopterReviewExpired_returns400() throws Exception {
            when(flightOrderService.create(any()))
                    .thenThrow(new HelicopterReviewExpiredException("Helicopter SP-HEL1 does not have a valid review for flight date 2026-06-15"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_DTO_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("does not have a valid review")));
        }

        @Test
        @DisplayName("pilot license expired — returns 400")
        void create_pilotLicenseExpired_returns400() throws Exception {
            when(flightOrderService.create(any()))
                    .thenThrow(new PilotLicenseExpiredException("Pilot Jan Pilotowski does not have a valid license for flight date 2026-06-15"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_DTO_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("does not have a valid license")));
        }

        @Test
        @DisplayName("crew training expired — returns 400")
        void create_crewTrainingExpired_returns400() throws Exception {
            when(flightOrderService.create(any()))
                    .thenThrow(new CrewMemberTrainingExpiredException("Crew member Jan Kowalski does not have valid training for flight date 2026-06-15"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_DTO_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("does not have valid training")));
        }

        @Test
        @DisplayName("crew weight exceeded — returns 400")
        void create_crewWeightExceeded_returns400() throws Exception {
            when(flightOrderService.create(any()))
                    .thenThrow(new CrewWeightExceededException("Crew weight 600 kg exceeds helicopter max payload 500 kg"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_DTO_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("exceeds helicopter max payload")));
        }

        @Test
        @DisplayName("route exceeds range — returns 400")
        void create_routeExceedsRange_returns400() throws Exception {
            when(flightOrderService.create(any()))
                    .thenThrow(new RouteExceedsRangeException("Estimated route 700 km exceeds helicopter range 600 km"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_DTO_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("exceeds helicopter range")));
        }

        @Test
        @DisplayName("pilot not found — returns 404")
        void create_pilotNotFound_returns404() throws Exception {
            when(flightOrderService.create(any()))
                    .thenThrow(new EntityNotFoundException("CrewMember", 999L));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_DTO_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("CrewMember with id 999 not found"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.aero.controller.FlightOrderControllerTest#createValidationCases")
        @DisplayName("validation error returns 400")
        void create_withInvalidField_returns400(String testName, String json, String expectedError) throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(expectedError)));
        }

        @Test
        @DisplayName("all required fields null — returns 400 with multiple errors")
        void create_withNullFields_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(5))));
        }
    }

    // ==================== UPDATE ====================

    @Nested
    @DisplayName("PUT /api/v1/flight-orders/{id}")
    class UpdateTests {

        @Test
        @DisplayName("happy path — returns 200")
        void update_returns200() throws Exception {
            when(flightOrderService.update(eq(1L), any()))
                    .thenReturn(sampleResponse(1L, FlightOrderStatus.INTRODUCED));

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_DTO_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("not found — returns 404")
        void update_notFound_returns404() throws Exception {
            when(flightOrderService.update(eq(999L), any()))
                    .thenThrow(new EntityNotFoundException("FlightOrder", 999L));

            mockMvc.perform(put(BASE_URL + "/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_DTO_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("FlightOrder with id 999 not found"));
        }

        @Test
        @DisplayName("validation error — missing helicopterId returns 400")
        void update_missingHelicopterId_returns400() throws Exception {
            String json = """
                    {
                        "plannedDeparture": "2026-06-15T08:00:00",
                        "plannedLanding": "2026-06-15T12:00:00",
                        "pilotId": 1,
                        "departureAirfieldId": 1,
                        "arrivalAirfieldId": 2,
                        "operationIds": [1],
                        "estimatedRouteKm": 150
                    }
                    """;

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem("Helicopter is required")));
        }
    }

    // ==================== SUBMIT ====================

    @Nested
    @DisplayName("POST /api/v1/flight-orders/{id}/submit")
    class SubmitTests {

        @Test
        @DisplayName("happy path — INTRODUCED → SUBMITTED returns 200")
        void submit_returns200() throws Exception {
            when(flightOrderService.submit(1L)).thenReturn(sampleResponse(1L, FlightOrderStatus.SUBMITTED));

            mockMvc.perform(post(BASE_URL + "/1/submit"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUBMITTED"));
        }

        @Test
        @DisplayName("wrong status — returns 400")
        void submit_wrongStatus_returns400() throws Exception {
            when(flightOrderService.submit(1L))
                    .thenThrow(new ValidationException("Can only submit flight orders in INTRODUCED status"));

            mockMvc.perform(post(BASE_URL + "/1/submit"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Can only submit flight orders in INTRODUCED status"));
        }

        @Test
        @DisplayName("not found — returns 404")
        void submit_notFound_returns404() throws Exception {
            when(flightOrderService.submit(999L))
                    .thenThrow(new EntityNotFoundException("FlightOrder", 999L));

            mockMvc.perform(post(BASE_URL + "/999/submit"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== REJECT ====================

    @Nested
    @DisplayName("POST /api/v1/flight-orders/{id}/reject")
    @WithMockUser(roles = "SUPERVISOR")
    class RejectTests {

        @Test
        @DisplayName("happy path — SUBMITTED → REJECTED returns 200")
        void reject_returns200() throws Exception {
            when(flightOrderService.reject(1L)).thenReturn(sampleResponse(1L, FlightOrderStatus.REJECTED));

            mockMvc.perform(post(BASE_URL + "/1/reject"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));
        }

        @Test
        @DisplayName("wrong status — returns 400")
        void reject_wrongStatus_returns400() throws Exception {
            when(flightOrderService.reject(1L))
                    .thenThrow(new ValidationException("Can only reject flight orders in SUBMITTED status"));

            mockMvc.perform(post(BASE_URL + "/1/reject"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Can only reject flight orders in SUBMITTED status"));
        }

        @Test
        @DisplayName("not found — returns 404")
        void reject_notFound_returns404() throws Exception {
            when(flightOrderService.reject(999L))
                    .thenThrow(new EntityNotFoundException("FlightOrder", 999L));

            mockMvc.perform(post(BASE_URL + "/999/reject"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== ACCEPT ====================

    @Nested
    @DisplayName("POST /api/v1/flight-orders/{id}/accept")
    @WithMockUser(roles = "SUPERVISOR")
    class AcceptTests {

        @Test
        @DisplayName("happy path — SUBMITTED → ACCEPTED returns 200")
        void accept_returns200() throws Exception {
            when(flightOrderService.accept(1L)).thenReturn(sampleResponse(1L, FlightOrderStatus.ACCEPTED));

            mockMvc.perform(post(BASE_URL + "/1/accept"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));
        }

        @Test
        @DisplayName("wrong status — returns 400")
        void accept_wrongStatus_returns400() throws Exception {
            when(flightOrderService.accept(1L))
                    .thenThrow(new ValidationException("Can only accept flight orders in SUBMITTED status"));

            mockMvc.perform(post(BASE_URL + "/1/accept"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Can only accept flight orders in SUBMITTED status"));
        }

        @Test
        @DisplayName("not found — returns 404")
        void accept_notFound_returns404() throws Exception {
            when(flightOrderService.accept(999L))
                    .thenThrow(new EntityNotFoundException("FlightOrder", 999L));

            mockMvc.perform(post(BASE_URL + "/999/accept"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== COMPLETE ====================

    @Nested
    @DisplayName("POST /api/v1/flight-orders/{id}/complete")
    class CompleteTests {

        @Test
        @DisplayName("DONE — happy path returns 200")
        void complete_done_returns200() throws Exception {
            when(flightOrderService.markDone(1L)).thenReturn(sampleResponse(1L, FlightOrderStatus.DONE));

            mockMvc.perform(post(BASE_URL + "/1/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"result": "Zrealizowane w całości"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DONE"));
        }

        @Test
        @DisplayName("Zrealizowane w części — happy path returns 200")
        void complete_partiallyDone_returns200() throws Exception {
            when(flightOrderService.markPartiallyDone(1L)).thenReturn(sampleResponse(1L, FlightOrderStatus.PARTIALLY_DONE));

            mockMvc.perform(post(BASE_URL + "/1/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"result": "Zrealizowane w części"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PARTIALLY_DONE"));
        }

        @Test
        @DisplayName("Nie zrealizowane — happy path returns 200")
        void complete_notDone_returns200() throws Exception {
            when(flightOrderService.markNotDone(1L)).thenReturn(sampleResponse(1L, FlightOrderStatus.NOT_DONE));

            mockMvc.perform(post(BASE_URL + "/1/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"result": "Nie zrealizowane"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("NOT_DONE"));
        }

        // TODO: odkomentować gdy frontend doda pola actualDeparture/actualLanding
        // @Test
        // @DisplayName("missing actual times — returns 400")
        // void complete_missingActualTimes_returns400() throws Exception {
        //     when(flightOrderService.markDone(1L))
        //             .thenThrow(new ValidationException("Actual departure and landing times are required before completing a flight order"));
        //
        //     mockMvc.perform(post(BASE_URL + "/1/complete")
        //                     .contentType(MediaType.APPLICATION_JSON)
        //                     .content("""
        //                             {"result": "Zrealizowane w całości"}
        //                             """))
        //             .andExpect(status().isBadRequest())
        //             .andExpect(jsonPath("$.message", containsString("Actual departure and landing times are required")));
        // }

        @Test
        @DisplayName("wrong status — returns 400")
        void complete_wrongStatus_returns400() throws Exception {
            when(flightOrderService.markDone(1L))
                    .thenThrow(new ValidationException("Can only mark as done flight orders in ACCEPTED status"));

            mockMvc.perform(post(BASE_URL + "/1/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"result": "Zrealizowane w całości"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Can only mark as done flight orders in ACCEPTED status"));
        }

        @Test
        @DisplayName("blank result — returns 400")
        void complete_blankResult_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"result": ""}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem("Result is required")));
        }

        @Test
        @DisplayName("null result — returns 400")
        void complete_nullResult_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem("Result is required")));
        }

        @Test
        @DisplayName("not found — returns 404")
        void complete_notFound_returns404() throws Exception {
            when(flightOrderService.markDone(999L))
                    .thenThrow(new EntityNotFoundException("FlightOrder", 999L));

            mockMvc.perform(post(BASE_URL + "/999/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"result": "Zrealizowane w całości"}
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== PARAMETERIZED TEST DATA ====================

    static Stream<Arguments> createValidationCases() {
        return Stream.of(
                Arguments.of("missing plannedDeparture",
                        """
                        {
                            "plannedLanding": "2026-06-15T12:00:00",
                            "pilotId": 1, "helicopterId": 1,
                            "departureAirfieldId": 1, "arrivalAirfieldId": 2,
                            "operationIds": [1], "estimatedRouteKm": 150
                        }
                        """,
                        "Planned departure is required"),
                Arguments.of("missing plannedLanding",
                        """
                        {
                            "plannedDeparture": "2026-06-15T08:00:00",
                            "pilotId": 1, "helicopterId": 1,
                            "departureAirfieldId": 1, "arrivalAirfieldId": 2,
                            "operationIds": [1], "estimatedRouteKm": 150
                        }
                        """,
                        "Planned landing is required"),
                Arguments.of("missing helicopterId",
                        """
                        {
                            "plannedDeparture": "2026-06-15T08:00:00",
                            "plannedLanding": "2026-06-15T12:00:00",
                            "pilotId": 1,
                            "departureAirfieldId": 1, "arrivalAirfieldId": 2,
                            "operationIds": [1], "estimatedRouteKm": 150
                        }
                        """,
                        "Helicopter is required"),
                Arguments.of("missing departureAirfieldId",
                        """
                        {
                            "plannedDeparture": "2026-06-15T08:00:00",
                            "plannedLanding": "2026-06-15T12:00:00",
                            "pilotId": 1, "helicopterId": 1,
                            "arrivalAirfieldId": 2,
                            "operationIds": [1], "estimatedRouteKm": 150
                        }
                        """,
                        "Departure airfield is required"),
                Arguments.of("missing arrivalAirfieldId",
                        """
                        {
                            "plannedDeparture": "2026-06-15T08:00:00",
                            "plannedLanding": "2026-06-15T12:00:00",
                            "pilotId": 1, "helicopterId": 1,
                            "departureAirfieldId": 1,
                            "operationIds": [1], "estimatedRouteKm": 150
                        }
                        """,
                        "Arrival airfield is required"),
                Arguments.of("empty operationIds",
                        """
                        {
                            "plannedDeparture": "2026-06-15T08:00:00",
                            "plannedLanding": "2026-06-15T12:00:00",
                            "pilotId": 1, "helicopterId": 1,
                            "departureAirfieldId": 1, "arrivalAirfieldId": 2,
                            "operationIds": [], "estimatedRouteKm": 150
                        }
                        """,
                        "At least one planned operation is required"),
                Arguments.of("missing estimatedRouteKm",
                        """
                        {
                            "plannedDeparture": "2026-06-15T08:00:00",
                            "plannedLanding": "2026-06-15T12:00:00",
                            "pilotId": 1, "helicopterId": 1,
                            "departureAirfieldId": 1, "arrivalAirfieldId": 2,
                            "operationIds": [1]
                        }
                        """,
                        "Estimated route km is required"),
                Arguments.of("estimatedRouteKm zero",
                        """
                        {
                            "plannedDeparture": "2026-06-15T08:00:00",
                            "plannedLanding": "2026-06-15T12:00:00",
                            "pilotId": 1, "helicopterId": 1,
                            "departureAirfieldId": 1, "arrivalAirfieldId": 2,
                            "operationIds": [1], "estimatedRouteKm": 0
                        }
                        """,
                        "Estimated route km must be at least 1")
        );
    }
}

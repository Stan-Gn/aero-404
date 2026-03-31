package com.aero.controller;

import com.aero.config.SecConf;
import com.aero.domain.FlightOrderStatus;
import com.aero.dto.FlightOrderResponseDto;
import com.aero.exception.GlobalExceptionHandler;
import com.aero.security.JwtTokenProvider;
import com.aero.service.FlightOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy bezpieczeństwa endpointów FlightOrderController zgodnie z PRD 7.2.
 *
 * Macierz dostępu (Zlecenia na lot):
 *   GET (podgląd):        ADMIN, PILOT, SUPERVISOR
 *   POST create:          PILOT
 *   PUT update:           PILOT, SUPERVISOR
 *   submit, complete:     PILOT
 *   reject, accept:       SUPERVISOR
 *   PLANNER:              brak dostępu (blokowany na poziomie HTTP)
 */
@WebMvcTest(FlightOrderController.class)
@Import({SecConf.class, GlobalExceptionHandler.class})
class FlightOrderControllerSecurityTest {

    private static final String BASE_URL = "/api/v1/flight-orders";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FlightOrderService flightOrderService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // --- Helpers ---

    private static FlightOrderResponseDto sampleResponse(Long id) {
        return new FlightOrderResponseDto(
                id, "FO-2026-ABCD1234",
                LocalDateTime.of(2026, 6, 1, 8, 0),
                LocalDateTime.of(2026, 6, 1, 12, 0),
                null, null, null, null, List.of(), null, null,
                List.of(), 240, 150, FlightOrderStatus.INTRODUCED);
    }

    private static final String VALID_JSON = """
            {
                "plannedDeparture": "2026-06-01T08:00:00",
                "plannedLanding": "2026-06-01T12:00:00",
                "helicopterId": 1,
                "departureAirfieldId": 1,
                "arrivalAirfieldId": 2,
                "operationIds": [1],
                "estimatedRouteKm": 150
            }
            """;

    private static final String COMPLETE_JSON = """
            { "result": "DONE" }
            """;

    // ==================== PILOT — pełny dostęp ====================

    @Nested
    @DisplayName("PILOT — pełny dostęp (PRD 7.2)")
    @WithMockUser(roles = "PILOT")
    class PilotAccess {

        @Test
        @DisplayName("GET /flight-orders → 200")
        void getAll_returns200() throws Exception {
            when(flightOrderService.findAll()).thenReturn(List.of(sampleResponse(1L)));
            mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /flight-orders/{id} → 200")
        void getById_returns200() throws Exception {
            when(flightOrderService.findById(1L)).thenReturn(sampleResponse(1L));
            mockMvc.perform(get(BASE_URL + "/1")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /flight-orders → 201")
        void create_returns201() throws Exception {
            when(flightOrderService.create(any())).thenReturn(sampleResponse(1L));
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("PUT /flight-orders/{id} → 200")
        void update_returns200() throws Exception {
            when(flightOrderService.update(eq(1L), any())).thenReturn(sampleResponse(1L));
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/submit → 200")
        void submit_returns200() throws Exception {
            when(flightOrderService.submit(1L)).thenReturn(sampleResponse(1L));
            mockMvc.perform(post(BASE_URL + "/1/submit")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/reject → 403")
        void reject_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/reject")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/accept → 403")
        void accept_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/accept")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/complete → 200")
        void complete_returns200() throws Exception {
            when(flightOrderService.markDone(1L)).thenReturn(sampleResponse(1L));
            mockMvc.perform(post(BASE_URL + "/1/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(COMPLETE_JSON))
                    .andExpect(status().isOk());
        }
    }

    // ==================== SUPERVISOR — edycja + reject/accept ====================

    @Nested
    @DisplayName("SUPERVISOR — edycja/podgląd + reject/accept (PRD 7.2)")
    @WithMockUser(roles = "SUPERVISOR")
    class SupervisorAccess {

        @Test
        @DisplayName("GET /flight-orders → 200")
        void getAll_returns200() throws Exception {
            when(flightOrderService.findAll()).thenReturn(List.of(sampleResponse(1L)));
            mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /flight-orders/{id} → 200")
        void getById_returns200() throws Exception {
            when(flightOrderService.findById(1L)).thenReturn(sampleResponse(1L));
            mockMvc.perform(get(BASE_URL + "/1")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /flight-orders → 403")
        void create_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /flight-orders/{id} → 200")
        void update_returns200() throws Exception {
            when(flightOrderService.update(eq(1L), any())).thenReturn(sampleResponse(1L));
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/submit → 403")
        void submit_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/submit")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/reject → 200")
        void reject_returns200() throws Exception {
            when(flightOrderService.reject(1L)).thenReturn(sampleResponse(1L));
            mockMvc.perform(post(BASE_URL + "/1/reject")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/accept → 200")
        void accept_returns200() throws Exception {
            when(flightOrderService.accept(1L)).thenReturn(sampleResponse(1L));
            mockMvc.perform(post(BASE_URL + "/1/accept")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/complete → 403")
        void complete_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(COMPLETE_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== ADMIN — tylko podgląd ====================

    @Nested
    @DisplayName("ADMIN — tylko podgląd (PRD 7.2)")
    @WithMockUser(roles = "ADMIN")
    class AdminAccess {

        @Test
        @DisplayName("GET /flight-orders → 200")
        void getAll_returns200() throws Exception {
            when(flightOrderService.findAll()).thenReturn(List.of(sampleResponse(1L)));
            mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /flight-orders/{id} → 200")
        void getById_returns200() throws Exception {
            when(flightOrderService.findById(1L)).thenReturn(sampleResponse(1L));
            mockMvc.perform(get(BASE_URL + "/1")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /flight-orders → 403")
        void create_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /flight-orders/{id} → 403")
        void update_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/submit → 403")
        void submit_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/submit")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/reject → 403")
        void reject_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/reject")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/accept → 403")
        void accept_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/accept")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/complete → 403")
        void complete_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(COMPLETE_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== PLANNER — brak dostępu ====================

    @Nested
    @DisplayName("PLANNER — brak dostępu (PRD 7.2)")
    @WithMockUser(roles = "PLANNER")
    class PlannerAccess {

        @Test
        @DisplayName("GET /flight-orders → 403")
        void getAll_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL)).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /flight-orders/{id} → 403")
        void getById_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL + "/1")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders → 403")
        void create_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /flight-orders/{id} → 403")
        void update_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/submit → 403")
        void submit_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/submit")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/reject → 403")
        void reject_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/reject")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/accept → 403")
        void accept_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/accept")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/complete → 403")
        void complete_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(COMPLETE_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== Brak ról — brak dostępu ====================

    @Nested
    @DisplayName("Brak ról — brak dostępu")
    @WithMockUser(roles = {})
    class NoRolesAccess {

        @Test
        @DisplayName("GET /flight-orders → 403")
        void getAll_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL)).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /flight-orders/{id} → 403")
        void getById_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL + "/1")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders → 403")
        void create_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /flight-orders/{id} → 403")
        void update_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/submit → 403")
        void submit_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/submit")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/reject → 403")
        void reject_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/reject")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/accept → 403")
        void accept_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/accept")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /flight-orders/{id}/complete → 403")
        void complete_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(COMPLETE_JSON))
                    .andExpect(status().isForbidden());
        }
    }
}

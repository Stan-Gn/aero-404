package com.aero.controller;

import com.aero.config.SecConf;
import com.aero.domain.CrewRole;
import com.aero.dto.CrewMemberResponseDto;
import com.aero.exception.GlobalExceptionHandler;
import com.aero.security.JwtTokenProvider;
import com.aero.service.CrewMemberService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy bezpieczeństwa endpointów CrewMemberController zgodnie z PRD 7.2.
 *
 * Macierz dostępu (Administracja — Członkowie załogi):
 *   ADMIN      → pełny dostęp (CRUD)
 *   SUPERVISOR → tylko odczyt (GET)
 *   PILOT      → tylko odczyt (GET)
 *   PLANNER    → brak dostępu
 */
@WebMvcTest(CrewMemberController.class)
@Import({SecConf.class, GlobalExceptionHandler.class})
class CrewMemberControllerSecurityTest {

    private static final String BASE_URL = "/api/v1/crew-members";

    private static final String VALID_JSON = """
            {
                "firstName": "Jan",
                "lastName": "Kowalski",
                "email": "jan.kowalski@aero.pl",
                "weight": 80,
                "role": "PILOT",
                "licenseNumber": "PL-12345",
                "licenseExpiry": "2027-12-31",
                "trainingExpiry": "2027-06-30"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrewMemberService crewMemberService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static CrewMemberResponseDto sampleResponse(Long id) {
        return new CrewMemberResponseDto(
                id, "Jan", "Kowalski", "jan.kowalski@aero.pl",
                80, CrewRole.PILOT, "PL-12345",
                LocalDate.of(2027, 12, 31),
                LocalDate.of(2027, 6, 30)
        );
    }

    // ==================== ADMIN — pełny dostęp ====================

    @Nested
    @DisplayName("ADMIN — pełny dostęp (PRD 7.2)")
    @WithMockUser(roles = "ADMIN")
    class AdminAccess {

        @Test
        @DisplayName("GET /crew-members → 200")
        void getAll_returns200() throws Exception {
            when(crewMemberService.findAll()).thenReturn(List.of(sampleResponse(1L)));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /crew-members/{id} → 200")
        void getById_returns200() throws Exception {
            when(crewMemberService.findById(1L)).thenReturn(sampleResponse(1L));

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /crew-members → 201")
        void create_returns201() throws Exception {
            when(crewMemberService.create(any())).thenReturn(sampleResponse(1L));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("PUT /crew-members/{id} → 200")
        void update_returns200() throws Exception {
            when(crewMemberService.update(eq(1L), any())).thenReturn(sampleResponse(1L));

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE /crew-members/{id} → 204")
        void delete_returns204() throws Exception {
            doNothing().when(crewMemberService).delete(1L);

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isNoContent());
        }
    }

    // ==================== SUPERVISOR — tylko odczyt ====================

    @Nested
    @DisplayName("SUPERVISOR — tylko odczyt (PRD 7.2)")
    @WithMockUser(roles = "SUPERVISOR")
    class SupervisorAccess {

        @Test
        @DisplayName("GET /crew-members → 200")
        void getAll_returns200() throws Exception {
            when(crewMemberService.findAll()).thenReturn(List.of(sampleResponse(1L)));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /crew-members/{id} → 200")
        void getById_returns200() throws Exception {
            when(crewMemberService.findById(1L)).thenReturn(sampleResponse(1L));

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /crew-members → 403")
        void create_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /crew-members/{id} → 403")
        void update_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /crew-members/{id} → 403")
        void delete_returns403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== PILOT — tylko odczyt ====================

    @Nested
    @DisplayName("PILOT — tylko odczyt (PRD 7.2)")
    @WithMockUser(roles = "PILOT")
    class PilotAccess {

        @Test
        @DisplayName("GET /crew-members → 200")
        void getAll_returns200() throws Exception {
            when(crewMemberService.findAll()).thenReturn(List.of(sampleResponse(1L)));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /crew-members/{id} → 200")
        void getById_returns200() throws Exception {
            when(crewMemberService.findById(1L)).thenReturn(sampleResponse(1L));

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /crew-members → 403")
        void create_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /crew-members/{id} → 403")
        void update_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /crew-members/{id} → 403")
        void delete_returns403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== PLANNER — brak dostępu ====================

    @Nested
    @DisplayName("PLANNER — brak dostępu (PRD 7.2)")
    @WithMockUser(roles = "PLANNER")
    class PlannerAccess {

        @Test
        @DisplayName("GET /crew-members → 403")
        void getAll_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /crew-members/{id} → 403")
        void getById_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /crew-members → 403")
        void create_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /crew-members/{id} → 403")
        void update_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /crew-members/{id} → 403")
        void delete_returns403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== Brak ról — brak dostępu ====================

    @Nested
    @DisplayName("Brak ról — brak dostępu")
    @WithMockUser(roles = {})
    class NoRolesAccess {

        @Test
        @DisplayName("GET /crew-members → 403")
        void getAll_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /crew-members/{id} → 403")
        void getById_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /crew-members → 403")
        void create_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /crew-members/{id} → 403")
        void update_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /crew-members/{id} → 403")
        void delete_returns403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isForbidden());
        }
    }
}

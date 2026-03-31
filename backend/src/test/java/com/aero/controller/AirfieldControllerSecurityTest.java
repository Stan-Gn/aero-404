package com.aero.controller;

import com.aero.config.SecConf;
import com.aero.dto.AirfieldResponseDto;
import com.aero.exception.GlobalExceptionHandler;
import com.aero.security.JwtTokenProvider;
import com.aero.service.AirfieldService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy bezpieczeństwa endpointów AirfieldController zgodnie z PRD 7.2.
 *
 * Macierz dostępu (Administracja — Lądowiska):
 *   ADMIN      → pełny dostęp (CRUD)
 *   SUPERVISOR → tylko odczyt (GET)
 *   PILOT      → tylko odczyt (GET)
 *   PLANNER    → brak dostępu
 */
@WebMvcTest(AirfieldController.class)
@Import({SecConf.class, GlobalExceptionHandler.class})
class AirfieldControllerSecurityTest {

    private static final String BASE_URL = "/api/v1/airfields";

    private static final String VALID_JSON = """
            {
                "name": "Lotnisko Wrocław",
                "latitude": 51.1,
                "longitude": 17.0
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AirfieldService airfieldService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static AirfieldResponseDto sampleResponse(Long id) {
        return new AirfieldResponseDto(id, "Lotnisko Wrocław", 51.1, 17.0);
    }

    // ==================== ADMIN — pełny dostęp ====================

    @Nested
    @DisplayName("ADMIN — pełny dostęp (PRD 7.2)")
    @WithMockUser(roles = "ADMIN")
    class AdminAccess {

        @Test
        @DisplayName("GET /airfields → 200")
        void getAll_returns200() throws Exception {
            when(airfieldService.findAll()).thenReturn(List.of(sampleResponse(1L)));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /airfields/{id} → 200")
        void getById_returns200() throws Exception {
            when(airfieldService.findById(1L)).thenReturn(sampleResponse(1L));

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /airfields → 201")
        void create_returns201() throws Exception {
            when(airfieldService.create(any())).thenReturn(sampleResponse(1L));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("PUT /airfields/{id} → 200")
        void update_returns200() throws Exception {
            when(airfieldService.update(eq(1L), any())).thenReturn(sampleResponse(1L));

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE /airfields/{id} → 204")
        void delete_returns204() throws Exception {
            doNothing().when(airfieldService).delete(1L);

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
        @DisplayName("GET /airfields → 200")
        void getAll_returns200() throws Exception {
            when(airfieldService.findAll()).thenReturn(List.of(sampleResponse(1L)));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /airfields/{id} → 200")
        void getById_returns200() throws Exception {
            when(airfieldService.findById(1L)).thenReturn(sampleResponse(1L));

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /airfields → 403")
        void create_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /airfields/{id} → 403")
        void update_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /airfields/{id} → 403")
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
        @DisplayName("GET /airfields → 200")
        void getAll_returns200() throws Exception {
            when(airfieldService.findAll()).thenReturn(List.of(sampleResponse(1L)));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /airfields/{id} → 200")
        void getById_returns200() throws Exception {
            when(airfieldService.findById(1L)).thenReturn(sampleResponse(1L));

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /airfields → 403")
        void create_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /airfields/{id} → 403")
        void update_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /airfields/{id} → 403")
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
        @DisplayName("GET /airfields → 403")
        void getAll_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /airfields/{id} → 403")
        void getById_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /airfields → 403")
        void create_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /airfields/{id} → 403")
        void update_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /airfields/{id} → 403")
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
        @DisplayName("GET /airfields → 403")
        void getAll_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /airfields/{id} → 403")
        void getById_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /airfields → 403")
        void create_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /airfields/{id} → 403")
        void update_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /airfields/{id} → 403")
        void delete_returns403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isForbidden());
        }
    }
}

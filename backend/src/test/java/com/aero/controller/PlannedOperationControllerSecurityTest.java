package com.aero.controller;

import com.aero.config.SecConf;
import com.aero.domain.ActivityType;
import com.aero.domain.OperationStatus;
import com.aero.domain.UserRole;
import com.aero.dto.OperationCommentDto;
import com.aero.dto.PlannedOperationResponseDto;
import com.aero.dto.UserSimpleDto;
import com.aero.exception.GlobalExceptionHandler;
import com.aero.security.JwtTokenProvider;
import com.aero.service.PlannedOperationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy bezpieczeństwa endpointów PlannedOperationController zgodnie z PRD 7.2.
 *
 * Macierz dostępu (Planowanie operacji):
 *   GET (podgląd):           ADMIN, PLANNER, SUPERVISOR, PILOT
 *   POST/PUT/DELETE (edycja): PLANNER, SUPERVISOR
 *   reject, confirm:          SUPERVISOR
 *   resign:                   PLANNER
 *   comments:                 ADMIN, PLANNER, SUPERVISOR, PILOT
 */
@WebMvcTest(PlannedOperationController.class)
@Import({SecConf.class, GlobalExceptionHandler.class})
class PlannedOperationControllerSecurityTest {

    private static final String BASE_URL = "/api/v1/operations";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlannedOperationService operationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // --- Helpers ---

    private static final UserSimpleDto SAMPLE_USER = new UserSimpleDto(
            1L, "Jan", "Kowalski", "jan@test.pl", UserRole.SUPERVISOR);

    private static PlannedOperationResponseDto sampleResponse(Long id) {
        return new PlannedOperationResponseDto(
                id, "OP-2026-ABCD1234", "ZAM-001", "Inspekcja mostu",
                "route.kml", 150, "[[51.1,17.0],[51.2,17.1]]",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 15),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10),
                Set.of(ActivityType.VISUAL_INSPECTION, ActivityType.PHOTOS),
                "Dodatkowe info", OperationStatus.INTRODUCED, SAMPLE_USER,
                "contact@test.pl", null, List.of(), List.of());
    }

    private static final String VALID_DTO_JSON = """
            {
                "orderNumber": "ZAM-001",
                "shortDescription": "Inspekcja mostu",
                "activityTypes": ["VISUAL_INSPECTION","PHOTOS"],
                "contactEmails": "contact@test.pl",
                "proposedDateFrom": "2026-05-01",
                "proposedDateTo": "2026-05-15"
            }
            """;

    private static MockMultipartFile dtoPart() {
        return new MockMultipartFile("dto", "", "application/json", VALID_DTO_JSON.getBytes());
    }

    private static MockMultipartFile kmlPart() {
        return new MockMultipartFile("kmlFile", "route.kml", "application/xml",
                "<kml><Document></Document></kml>".getBytes());
    }

    private static final String COMMENT_JSON = """
            { "text": "Komentarz testowy" }
            """;

    // ==================== SUPERVISOR — pełny dostęp ====================

    @Nested
    @DisplayName("SUPERVISOR — pełny dostęp (PRD 7.2)")
    @WithMockUser(roles = "SUPERVISOR")
    class SupervisorAccess {

        @Test
        @DisplayName("GET /operations → 200")
        void getAll_returns200() throws Exception {
            when(operationService.findAll()).thenReturn(List.of(sampleResponse(1L)));
            mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /operations/{id} → 200")
        void getById_returns200() throws Exception {
            when(operationService.findById(1L)).thenReturn(sampleResponse(1L));
            mockMvc.perform(get(BASE_URL + "/1")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /operations → 201")
        void create_returns201() throws Exception {
            when(operationService.create(any(), any())).thenReturn(sampleResponse(1L));
            mockMvc.perform(multipart(BASE_URL).file(dtoPart()).file(kmlPart()))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("PUT /operations/{id} → 200")
        void update_returns200() throws Exception {
            when(operationService.update(eq(1L), any(), any())).thenReturn(sampleResponse(1L));
            mockMvc.perform(multipart(BASE_URL + "/1").file(dtoPart()).file(kmlPart())
                            .with(req -> { req.setMethod("PUT"); return req; }))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE /operations/{id} → 204")
        void delete_returns204() throws Exception {
            doNothing().when(operationService).delete(1L);
            mockMvc.perform(delete(BASE_URL + "/1")).andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("POST /operations/{id}/reject → 200")
        void reject_returns200() throws Exception {
            when(operationService.reject(1L)).thenReturn(sampleResponse(1L));
            mockMvc.perform(post(BASE_URL + "/1/reject")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /operations/{id}/confirm → 200")
        void confirm_returns200() throws Exception {
            when(operationService.confirmToPlan(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(sampleResponse(1L));
            mockMvc.perform(post(BASE_URL + "/1/confirm")
                            .param("plannedDateFrom", "2026-06-01")
                            .param("plannedDateTo", "2026-06-10"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /operations/{id}/resign → 403")
        void resign_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/resign")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations/{id}/comments → 201")
        void addComment_returns201() throws Exception {
            when(operationService.addComment(eq(1L), any()))
                    .thenReturn(new OperationCommentDto(1L, "Komentarz", SAMPLE_USER, LocalDateTime.now()));
            mockMvc.perform(post(BASE_URL + "/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(COMMENT_JSON))
                    .andExpect(status().isCreated());
        }
    }

    // ==================== PLANNER — tworzenie/edycja + resign ====================

    @Nested
    @DisplayName("PLANNER — tworzenie/edycja/resign (PRD 7.2)")
    @WithMockUser(roles = "PLANNER")
    class PlannerAccess {

        @Test
        @DisplayName("GET /operations → 200")
        void getAll_returns200() throws Exception {
            when(operationService.findAll()).thenReturn(List.of(sampleResponse(1L)));
            mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /operations/{id} → 200")
        void getById_returns200() throws Exception {
            when(operationService.findById(1L)).thenReturn(sampleResponse(1L));
            mockMvc.perform(get(BASE_URL + "/1")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /operations → 201")
        void create_returns201() throws Exception {
            when(operationService.create(any(), any())).thenReturn(sampleResponse(1L));
            mockMvc.perform(multipart(BASE_URL).file(dtoPart()).file(kmlPart()))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("PUT /operations/{id} → 200")
        void update_returns200() throws Exception {
            when(operationService.update(eq(1L), any(), any())).thenReturn(sampleResponse(1L));
            mockMvc.perform(multipart(BASE_URL + "/1").file(dtoPart()).file(kmlPart())
                            .with(req -> { req.setMethod("PUT"); return req; }))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE /operations/{id} → 204")
        void delete_returns204() throws Exception {
            doNothing().when(operationService).delete(1L);
            mockMvc.perform(delete(BASE_URL + "/1")).andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("POST /operations/{id}/reject → 403")
        void reject_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/reject")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations/{id}/confirm → 403")
        void confirm_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/confirm")
                            .param("plannedDateFrom", "2026-06-01")
                            .param("plannedDateTo", "2026-06-10"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations/{id}/resign → 200")
        void resign_returns200() throws Exception {
            when(operationService.resign(1L)).thenReturn(sampleResponse(1L));
            mockMvc.perform(post(BASE_URL + "/1/resign")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /operations/{id}/comments → 201")
        void addComment_returns201() throws Exception {
            when(operationService.addComment(eq(1L), any()))
                    .thenReturn(new OperationCommentDto(1L, "Komentarz", SAMPLE_USER, LocalDateTime.now()));
            mockMvc.perform(post(BASE_URL + "/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(COMMENT_JSON))
                    .andExpect(status().isCreated());
        }
    }

    // ==================== ADMIN — podgląd + komentarze ====================

    @Nested
    @DisplayName("ADMIN — podgląd + komentarze (PRD 7.2)")
    @WithMockUser(roles = "ADMIN")
    class AdminAccess {

        @Test
        @DisplayName("GET /operations → 200")
        void getAll_returns200() throws Exception {
            when(operationService.findAll()).thenReturn(List.of(sampleResponse(1L)));
            mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /operations/{id} → 200")
        void getById_returns200() throws Exception {
            when(operationService.findById(1L)).thenReturn(sampleResponse(1L));
            mockMvc.perform(get(BASE_URL + "/1")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /operations → 403")
        void create_returns403() throws Exception {
            mockMvc.perform(multipart(BASE_URL).file(dtoPart()).file(kmlPart()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /operations/{id} → 403")
        void update_returns403() throws Exception {
            mockMvc.perform(multipart(BASE_URL + "/1").file(dtoPart()).file(kmlPart())
                            .with(req -> { req.setMethod("PUT"); return req; }))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /operations/{id} → 403")
        void delete_returns403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations/{id}/reject → 403")
        void reject_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/reject")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations/{id}/confirm → 403")
        void confirm_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/confirm")
                            .param("plannedDateFrom", "2026-06-01")
                            .param("plannedDateTo", "2026-06-10"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations/{id}/resign → 403")
        void resign_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/resign")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations/{id}/comments → 201")
        void addComment_returns201() throws Exception {
            when(operationService.addComment(eq(1L), any()))
                    .thenReturn(new OperationCommentDto(1L, "Komentarz", SAMPLE_USER, LocalDateTime.now()));
            mockMvc.perform(post(BASE_URL + "/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(COMMENT_JSON))
                    .andExpect(status().isCreated());
        }
    }

    // ==================== PILOT — podgląd + komentarze ====================

    @Nested
    @DisplayName("PILOT — podgląd + komentarze (PRD 7.2)")
    @WithMockUser(roles = "PILOT")
    class PilotAccess {

        @Test
        @DisplayName("GET /operations → 200")
        void getAll_returns200() throws Exception {
            when(operationService.findAll()).thenReturn(List.of(sampleResponse(1L)));
            mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /operations/{id} → 200")
        void getById_returns200() throws Exception {
            when(operationService.findById(1L)).thenReturn(sampleResponse(1L));
            mockMvc.perform(get(BASE_URL + "/1")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /operations → 403")
        void create_returns403() throws Exception {
            mockMvc.perform(multipart(BASE_URL).file(dtoPart()).file(kmlPart()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /operations/{id} → 403")
        void update_returns403() throws Exception {
            mockMvc.perform(multipart(BASE_URL + "/1").file(dtoPart()).file(kmlPart())
                            .with(req -> { req.setMethod("PUT"); return req; }))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /operations/{id} → 403")
        void delete_returns403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations/{id}/reject → 403")
        void reject_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/reject")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations/{id}/confirm → 403")
        void confirm_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/confirm")
                            .param("plannedDateFrom", "2026-06-01")
                            .param("plannedDateTo", "2026-06-10"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations/{id}/resign → 403")
        void resign_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/resign")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations/{id}/comments → 201")
        void addComment_returns201() throws Exception {
            when(operationService.addComment(eq(1L), any()))
                    .thenReturn(new OperationCommentDto(1L, "Komentarz", SAMPLE_USER, LocalDateTime.now()));
            mockMvc.perform(post(BASE_URL + "/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(COMMENT_JSON))
                    .andExpect(status().isCreated());
        }
    }

    // ==================== Brak ról — brak dostępu ====================

    @Nested
    @DisplayName("Brak ról — brak dostępu")
    @WithMockUser(roles = {})
    class NoRolesAccess {

        @Test
        @DisplayName("GET /operations → 403")
        void getAll_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL)).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /operations/{id} → 403")
        void getById_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL + "/1")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations → 403")
        void create_returns403() throws Exception {
            mockMvc.perform(multipart(BASE_URL).file(dtoPart()).file(kmlPart()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PUT /operations/{id} → 403")
        void update_returns403() throws Exception {
            mockMvc.perform(multipart(BASE_URL + "/1").file(dtoPart()).file(kmlPart())
                            .with(req -> { req.setMethod("PUT"); return req; }))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /operations/{id} → 403")
        void delete_returns403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations/{id}/reject → 403")
        void reject_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/reject")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations/{id}/confirm → 403")
        void confirm_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/confirm")
                            .param("plannedDateFrom", "2026-06-01")
                            .param("plannedDateTo", "2026-06-10"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations/{id}/resign → 403")
        void resign_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/resign")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /operations/{id}/comments → 403")
        void addComment_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(COMMENT_JSON))
                    .andExpect(status().isForbidden());
        }
    }
}

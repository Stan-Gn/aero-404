package com.aero.controller;

import com.aero.config.SecConf;
import com.aero.domain.ActivityType;
import com.aero.domain.OperationStatus;
import com.aero.domain.UserRole;
import com.aero.dto.OperationCommentDto;
import com.aero.dto.PlannedOperationResponseDto;
import com.aero.dto.UserSimpleDto;
import com.aero.exception.EntityNotFoundException;
import com.aero.exception.GlobalExceptionHandler;
import com.aero.exception.ValidationException;
import com.aero.security.JwtTokenProvider;
import com.aero.service.PlannedOperationService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlannedOperationController.class)
@Import({SecConf.class, GlobalExceptionHandler.class})
@WithMockUser(roles = "SUPERVISOR")
class PlannedOperationControllerTest {

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

    private static PlannedOperationResponseDto sampleResponse(Long id, OperationStatus status) {
        return new PlannedOperationResponseDto(
                id, "OP-2026-ABCD1234", "ZAM-001", "Inspekcja mostu",
                "route.kml", 150, "[[51.1,17.0],[51.2,17.1]]",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 15),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10),
                Set.of(ActivityType.VISUAL_INSPECTION, ActivityType.PHOTOS),
                "Dodatkowe info", status, SAMPLE_USER,
                "contact@test.pl", null, List.of(), List.of());
    }

    private static String dtoJson(String orderNumber, String shortDescription,
                                  String activityTypes, String additionalInfo,
                                  String contactEmails,
                                  String proposedDateFrom, String proposedDateTo) {
        return """
                {
                    "orderNumber": %s,
                    "shortDescription": %s,
                    "activityTypes": %s,
                    "additionalInfo": %s,
                    "contactEmails": %s,
                    "proposedDateFrom": %s,
                    "proposedDateTo": %s
                }
                """.formatted(
                str(orderNumber), str(shortDescription),
                activityTypes != null ? activityTypes : "null",
                str(additionalInfo), str(contactEmails),
                str(proposedDateFrom), str(proposedDateTo));
    }

    private static String str(String val) {
        return val == null ? "null" : "\"" + val + "\"";
    }

    private static final String VALID_DTO_JSON = dtoJson(
            "ZAM-001", "Inspekcja mostu",
            "[\"VISUAL_INSPECTION\",\"PHOTOS\"]",
            null, "contact@test.pl", "2026-05-01", "2026-05-15");

    private static MockMultipartFile dtoPart(String json) {
        return new MockMultipartFile("dto", "", "application/json", json.getBytes());
    }

    private static MockMultipartFile kmlPart() {
        return new MockMultipartFile("kmlFile", "route.kml", "application/xml",
                "<kml><Document></Document></kml>".getBytes());
    }

    // ==================== GET ALL ====================

    @Nested
    @DisplayName("GET /api/v1/operations")
    class GetAllTests {

        @Test
        @DisplayName("returns 200 with list")
        void getAll_returns200() throws Exception {
            when(operationService.findAll()).thenReturn(List.of(
                    sampleResponse(1L, OperationStatus.CONFIRMED)));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].autoNumber").value("OP-2026-ABCD1234"));
        }

        @Test
        @DisplayName("with status filter returns 200")
        void getAll_withStatusFilter_returns200() throws Exception {
            when(operationService.findAll(OperationStatus.INTRODUCED)).thenReturn(List.of(
                    sampleResponse(1L, OperationStatus.INTRODUCED)));

            mockMvc.perform(get(BASE_URL).param("status", "INTRODUCED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("INTRODUCED"));
        }

        @Test
        @DisplayName("returns 200 with empty list")
        void getAll_empty_returns200() throws Exception {
            when(operationService.findAll()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ==================== GET BY ID ====================

    @Nested
    @DisplayName("GET /api/v1/operations/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("happy path — returns 200")
        void getById_returns200() throws Exception {
            when(operationService.findById(1L)).thenReturn(sampleResponse(1L, OperationStatus.CONFIRMED));

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.orderNumber").value("ZAM-001"))
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.routeKm").value(150));
        }

        @Test
        @DisplayName("not found — returns 404")
        void getById_notFound_returns404() throws Exception {
            when(operationService.findById(999L))
                    .thenThrow(new EntityNotFoundException("PlannedOperation", 999L));

            mockMvc.perform(get(BASE_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("PlannedOperation with id 999 not found"));
        }
    }

    // ==================== CREATE (multipart) ====================

    @Nested
    @DisplayName("POST /api/v1/operations (multipart)")
    class CreateTests {

        @Test
        @DisplayName("happy path — valid dto + kml returns 201")
        void create_returns201() throws Exception {
            when(operationService.create(any(), any())).thenReturn(sampleResponse(1L, OperationStatus.INTRODUCED));

            mockMvc.perform(multipart(BASE_URL)
                            .file(dtoPart(VALID_DTO_JSON))
                            .file(kmlPart()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("INTRODUCED"));
        }

        @Test
        @DisplayName("missing KML — service throws ValidationException → 400")
        void create_missingKml_returns400() throws Exception {
            when(operationService.create(any(), any()))
                    .thenThrow(new ValidationException("KML file is required for planned operation"));

            mockMvc.perform(multipart(BASE_URL)
                            .file(dtoPart(VALID_DTO_JSON)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("KML file is required for planned operation"));
        }

        @Test
        @DisplayName("proposedDateFrom after proposedDateTo — service throws ValidationException → 400")
        void create_invalidDates_returns400() throws Exception {
            when(operationService.create(any(), any()))
                    .thenThrow(new ValidationException("Proposed date from must be before proposed date to"));

            String json = dtoJson("ZAM-001", "Inspekcja", "[\"PHOTOS\"]",
                    null, null, "2026-06-15", "2026-06-01");

            mockMvc.perform(multipart(BASE_URL)
                            .file(dtoPart(json))
                            .file(kmlPart()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Proposed date from must be before proposed date to"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.aero.controller.PlannedOperationControllerTest#createValidationCases")
        @DisplayName("validation error returns 400")
        void create_withInvalidField_returns400(String testName, String json, String expectedError) throws Exception {
            mockMvc.perform(multipart(BASE_URL)
                            .file(dtoPart(json))
                            .file(kmlPart()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(expectedError)));
        }

        @Test
        @DisplayName("all fields null — returns 400 with multiple errors")
        void create_withNullFields_returns400() throws Exception {
            mockMvc.perform(multipart(BASE_URL)
                            .file(dtoPart("{}"))
                            .file(kmlPart()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(3))));
        }
    }

    // ==================== UPDATE (multipart PUT) ====================

    @Nested
    @DisplayName("PUT /api/v1/operations/{id} (multipart)")
    class UpdateTests {

        @Test
        @DisplayName("happy path — returns 200")
        void update_returns200() throws Exception {
            when(operationService.update(eq(1L), any(), any()))
                    .thenReturn(sampleResponse(1L, OperationStatus.INTRODUCED));

            mockMvc.perform(multipart(BASE_URL + "/1")
                            .file(dtoPart(VALID_DTO_JSON))
                            .file(kmlPart())
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("not found — returns 404")
        void update_notFound_returns404() throws Exception {
            when(operationService.update(eq(999L), any(), any()))
                    .thenThrow(new EntityNotFoundException("PlannedOperation", 999L));

            mockMvc.perform(multipart(BASE_URL + "/999")
                            .file(dtoPart(VALID_DTO_JSON))
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("PlannedOperation with id 999 not found"));
        }

        @Test
        @DisplayName("PLANNER editing other's operation — returns 400")
        void update_plannerNotOwner_returns400() throws Exception {
            when(operationService.update(eq(1L), any(), any()))
                    .thenThrow(new ValidationException("PLANNER can only edit own operations"));

            mockMvc.perform(multipart(BASE_URL + "/1")
                            .file(dtoPart(VALID_DTO_JSON))
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("PLANNER can only edit own operations"));
        }

        @Test
        @DisplayName("validation error — blank orderNumber returns 400")
        void update_blankOrderNumber_returns400() throws Exception {
            String json = dtoJson("", "Opis", "[\"PHOTOS\"]", null, null, null, null);

            mockMvc.perform(multipart(BASE_URL + "/1")
                            .file(dtoPart(json))
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem("Order number is required")));
        }
    }

    // ==================== DELETE ====================

    @Nested
    @DisplayName("DELETE /api/v1/operations/{id}")
    class DeleteTests {

        @Test
        @DisplayName("happy path — returns 204")
        void delete_returns204() throws Exception {
            doNothing().when(operationService).delete(1L);

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("not found — returns 404")
        void delete_notFound_returns404() throws Exception {
            doThrow(new EntityNotFoundException("PlannedOperation", 999L))
                    .when(operationService).delete(999L);

            mockMvc.perform(delete(BASE_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("PlannedOperation with id 999 not found"));
        }
    }

    // ==================== REJECT ====================

    @Nested
    @DisplayName("POST /api/v1/operations/{id}/reject")
    class RejectTests {

        @Test
        @DisplayName("happy path — INTRODUCED → REJECTED returns 200")
        void reject_returns200() throws Exception {
            when(operationService.reject(1L)).thenReturn(sampleResponse(1L, OperationStatus.REJECTED));

            mockMvc.perform(post(BASE_URL + "/1/reject"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));
        }

        @Test
        @DisplayName("wrong status — returns 400")
        void reject_wrongStatus_returns400() throws Exception {
            when(operationService.reject(1L))
                    .thenThrow(new ValidationException("Can only reject operations in INTRODUCED status"));

            mockMvc.perform(post(BASE_URL + "/1/reject"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Can only reject operations in INTRODUCED status"));
        }

        @Test
        @DisplayName("not found — returns 404")
        void reject_notFound_returns404() throws Exception {
            when(operationService.reject(999L))
                    .thenThrow(new EntityNotFoundException("PlannedOperation", 999L));

            mockMvc.perform(post(BASE_URL + "/999/reject"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== CONFIRM ====================

    @Nested
    @DisplayName("POST /api/v1/operations/{id}/confirm")
    class ConfirmTests {

        @Test
        @DisplayName("happy path — with planned dates returns 200")
        void confirm_returns200() throws Exception {
            when(operationService.confirmToPlan(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(sampleResponse(1L, OperationStatus.CONFIRMED));

            mockMvc.perform(post(BASE_URL + "/1/confirm")
                            .param("plannedDateFrom", "2026-06-01")
                            .param("plannedDateTo", "2026-06-10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("missing planned dates — returns error (not 2xx)")
        void confirm_missingDates_returnsError() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/confirm"))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("wrong status — returns 400")
        void confirm_wrongStatus_returns400() throws Exception {
            when(operationService.confirmToPlan(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenThrow(new ValidationException("Can only confirm operations in INTRODUCED status"));

            mockMvc.perform(post(BASE_URL + "/1/confirm")
                            .param("plannedDateFrom", "2026-06-01")
                            .param("plannedDateTo", "2026-06-10"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Can only confirm operations in INTRODUCED status"));
        }

        @Test
        @DisplayName("dateFrom after dateTo — returns 400")
        void confirm_invalidDateRange_returns400() throws Exception {
            when(operationService.confirmToPlan(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .thenThrow(new ValidationException("Planned date from must be before planned date to"));

            mockMvc.perform(post(BASE_URL + "/1/confirm")
                            .param("plannedDateFrom", "2026-06-15")
                            .param("plannedDateTo", "2026-06-01"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Planned date from must be before planned date to"));
        }

        @Test
        @DisplayName("not found — returns 404")
        void confirm_notFound_returns404() throws Exception {
            when(operationService.confirmToPlan(eq(999L), any(LocalDate.class), any(LocalDate.class)))
                    .thenThrow(new EntityNotFoundException("PlannedOperation", 999L));

            mockMvc.perform(post(BASE_URL + "/999/confirm")
                            .param("plannedDateFrom", "2026-06-01")
                            .param("plannedDateTo", "2026-06-10"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== RESIGN ====================

    @Nested
    @DisplayName("POST /api/v1/operations/{id}/resign")
    @WithMockUser(roles = "PLANNER")
    class ResignTests {

        @Test
        @DisplayName("happy path — returns 200")
        void resign_returns200() throws Exception {
            when(operationService.resign(1L)).thenReturn(sampleResponse(1L, OperationStatus.RESIGNED));

            mockMvc.perform(post(BASE_URL + "/1/resign"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESIGNED"));
        }

        @Test
        @DisplayName("wrong status — returns 400")
        void resign_wrongStatus_returns400() throws Exception {
            when(operationService.resign(1L))
                    .thenThrow(new ValidationException("Can only resign from operations in INTRODUCED, CONFIRMED, or SCHEDULED status"));

            mockMvc.perform(post(BASE_URL + "/1/resign"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            "Can only resign from operations in INTRODUCED, CONFIRMED, or SCHEDULED status"));
        }

        @Test
        @DisplayName("not found — returns 404")
        void resign_notFound_returns404() throws Exception {
            when(operationService.resign(999L))
                    .thenThrow(new EntityNotFoundException("PlannedOperation", 999L));

            mockMvc.perform(post(BASE_URL + "/999/resign"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== ADD COMMENT ====================

    @Nested
    @DisplayName("POST /api/v1/operations/{id}/comments")
    class AddCommentTests {

        @Test
        @DisplayName("happy path — valid comment returns 201")
        void addComment_returns201() throws Exception {
            when(operationService.addComment(eq(1L), any()))
                    .thenReturn(new OperationCommentDto(1L, "Dobra robota", SAMPLE_USER, LocalDateTime.now()));

            mockMvc.perform(post(BASE_URL + "/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"text": "Dobra robota"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.text").value("Dobra robota"));
        }

        @Test
        @DisplayName("blank text — returns 400")
        void addComment_blankText_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"text": ""}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem("Comment text is required")));
        }

        @Test
        @DisplayName("text too long (501 chars) — returns 400")
        void addComment_textTooLong_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"text\": \"" + "A".repeat(501) + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem("Comment must be at most 500 characters")));
        }

        @Test
        @DisplayName("null text — returns 400")
        void addComment_nullText_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem("Comment text is required")));
        }

        @Test
        @DisplayName("operation not found — returns 404")
        void addComment_notFound_returns404() throws Exception {
            when(operationService.addComment(eq(999L), any()))
                    .thenThrow(new EntityNotFoundException("PlannedOperation", 999L));

            mockMvc.perform(post(BASE_URL + "/999/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"text": "Komentarz"}
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    // NOTE: @PreAuthorize on controller methods is NOT enforced because
    // @EnableMethodSecurity is not configured in SecConf. Role-based access
    // tests (403 scenarios) are skipped until that is enabled.

    // ==================== PARAMETERIZED TEST DATA ====================

    static Stream<Arguments> createValidationCases() {
        return Stream.of(
                Arguments.of("blank orderNumber",
                        dtoJson("", "Opis", "[\"PHOTOS\"]", null, null, null, null),
                        "Order number is required"),
                Arguments.of("orderNumber too long",
                        dtoJson("O".repeat(31), "Opis", "[\"PHOTOS\"]", null, null, null, null),
                        "Order number must be at most 30 characters"),
                Arguments.of("blank shortDescription",
                        dtoJson("ZAM-001", "", "[\"PHOTOS\"]", null, null, null, null),
                        "Short description is required"),
                Arguments.of("shortDescription too long",
                        dtoJson("ZAM-001", "D".repeat(101), "[\"PHOTOS\"]", null, null, null, null),
                        "Short description must be at most 100 characters"),
                Arguments.of("empty activityTypes",
                        dtoJson("ZAM-001", "Opis", "[]", null, null, null, null),
                        "At least one activity type is required"),
                Arguments.of("additionalInfo too long",
                        dtoJson("ZAM-001", "Opis", "[\"PHOTOS\"]", "I".repeat(501), null, null, null),
                        "Additional info must be at most 500 characters"),
                Arguments.of("contactEmails too long",
                        dtoJson("ZAM-001", "Opis", "[\"PHOTOS\"]", null, "E".repeat(501), null, null),
                        "Contact emails must be at most 500 characters")
        );
    }
}

package com.aero.controller;

import com.aero.config.SecConf;
import com.aero.domain.HelicopterStatus;
import com.aero.dto.HelicopterResponseDto;
import com.aero.exception.EntityNotFoundException;
import com.aero.exception.GlobalExceptionHandler;
import com.aero.exception.ValidationException;
import com.aero.security.JwtTokenProvider;
import com.aero.service.HelicopterService;
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
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HelicopterController.class)
@Import({SecConf.class, GlobalExceptionHandler.class})
@WithMockUser(roles = "ADMIN")
class HelicopterControllerTest {

    private static final String BASE_URL = "/api/v1/helicopters";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HelicopterService helicopterService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // --- Helpers ---

    private static String heliJson(String regNumber, String type, String description,
                                   String maxCrew, String maxPayload, String status,
                                   String reviewDate, String rangeKm) {
        return """
                {
                    "regNumber": %s,
                    "type": %s,
                    "description": %s,
                    "maxCrew": %s,
                    "maxPayload": %s,
                    "status": %s,
                    "reviewDate": %s,
                    "rangeKm": %s
                }
                """.formatted(
                str(regNumber), str(type), str(description),
                maxCrew, maxPayload, str(status),
                str(reviewDate), rangeKm
        );
    }

    private static String str(String val) {
        return val == null ? "null" : "\"" + val + "\"";
    }

    private static final String VALID_ACTIVE_JSON = heliJson(
            "SP-ABC", "Mi-8", "Transport", "5", "500", "ACTIVE", "2027-06-01", "400");

    private static final String VALID_INACTIVE_JSON = heliJson(
            "SP-XYZ", "Robinson R44", null, "3", "200", "INACTIVE", null, "300");

    private static HelicopterResponseDto sampleActiveResponse(Long id) {
        return new HelicopterResponseDto(id, "SP-ABC", "Mi-8", "Transport",
                5, 500, HelicopterStatus.ACTIVE, LocalDate.of(2027, 6, 1), 400);
    }

    private static HelicopterResponseDto sampleInactiveResponse(Long id) {
        return new HelicopterResponseDto(id, "SP-XYZ", "Robinson R44", null,
                3, 200, HelicopterStatus.INACTIVE, null, 300);
    }

    // ==================== GET ALL ====================

    @Nested
    @DisplayName("GET /api/v1/helicopters")
    class GetAllTests {

        @Test
        @DisplayName("returns 200 with list")
        void getAll_returns200() throws Exception {
            when(helicopterService.findAll()).thenReturn(List.of(
                    sampleActiveResponse(1L), sampleInactiveResponse(2L)));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].regNumber").value("SP-ABC"))
                    .andExpect(jsonPath("$[1].status").value("INACTIVE"));
        }

        @Test
        @DisplayName("returns 200 with empty list")
        void getAll_empty_returns200() throws Exception {
            when(helicopterService.findAll()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ==================== GET BY ID ====================

    @Nested
    @DisplayName("GET /api/v1/helicopters/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("happy path — returns 200")
        void getById_returns200() throws Exception {
            when(helicopterService.findById(1L)).thenReturn(sampleActiveResponse(1L));

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.regNumber").value("SP-ABC"))
                    .andExpect(jsonPath("$.type").value("Mi-8"))
                    .andExpect(jsonPath("$.maxCrew").value(5))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.rangeKm").value(400));
        }

        @Test
        @DisplayName("not found — returns 404")
        void getById_notFound_returns404() throws Exception {
            when(helicopterService.findById(999L))
                    .thenThrow(new EntityNotFoundException("Helicopter", 999L));

            mockMvc.perform(get(BASE_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Helicopter with id 999 not found"));
        }
    }

    // ==================== CREATE ====================

    @Nested
    @DisplayName("POST /api/v1/helicopters")
    class CreateTests {

        @Test
        @DisplayName("happy path — ACTIVE with reviewDate returns 201")
        void create_active_returns201() throws Exception {
            when(helicopterService.create(any())).thenReturn(sampleActiveResponse(1L));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_ACTIVE_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.regNumber").value("SP-ABC"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("happy path — INACTIVE without reviewDate returns 201")
        void create_inactive_returns201() throws Exception {
            when(helicopterService.create(any())).thenReturn(sampleInactiveResponse(1L));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_INACTIVE_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("INACTIVE"))
                    .andExpect(jsonPath("$.reviewDate").isEmpty());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.aero.controller.HelicopterControllerTest#createValidationCases")
        @DisplayName("validation error returns 400")
        void create_withInvalidField_returns400(String testName, String json, String expectedError) throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors", hasItem(expectedError)));
        }

        @Test
        @DisplayName("all fields null — returns 400 with multiple errors")
        void create_withNullFields_returns400() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(5))));
        }

        @Test
        @DisplayName("duplicate regNumber — service throws ValidationException → 400")
        void create_duplicateRegNumber_returns400() throws Exception {
            when(helicopterService.create(any()))
                    .thenThrow(new ValidationException("Helicopter with registration number SP-ABC already exists"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_ACTIVE_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Helicopter with registration number SP-ABC already exists"));
        }

        @Test
        @DisplayName("ACTIVE without reviewDate — service throws ValidationException → 400")
        void create_activeNoReviewDate_returns400() throws Exception {
            when(helicopterService.create(any()))
                    .thenThrow(new ValidationException("Review date is required when helicopter status is ACTIVE"));

            String json = heliJson("SP-ABC", "Mi-8", null, "5", "500", "ACTIVE", null, "400");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Review date is required when helicopter status is ACTIVE"));
        }
    }

    // ==================== UPDATE ====================

    @Nested
    @DisplayName("PUT /api/v1/helicopters/{id}")
    class UpdateTests {

        @Test
        @DisplayName("happy path — returns 200")
        void update_returns200() throws Exception {
            when(helicopterService.update(eq(1L), any())).thenReturn(sampleActiveResponse(1L));

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_ACTIVE_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.regNumber").value("SP-ABC"));
        }

        @Test
        @DisplayName("not found — returns 404")
        void update_notFound_returns404() throws Exception {
            when(helicopterService.update(eq(999L), any()))
                    .thenThrow(new EntityNotFoundException("Helicopter", 999L));

            mockMvc.perform(put(BASE_URL + "/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_ACTIVE_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Helicopter with id 999 not found"));
        }

        @Test
        @DisplayName("duplicate regNumber on update — returns 400")
        void update_duplicateRegNumber_returns400() throws Exception {
            when(helicopterService.update(eq(1L), any()))
                    .thenThrow(new ValidationException("Helicopter with registration number SP-ABC already exists"));

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_ACTIVE_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Helicopter with registration number SP-ABC already exists"));
        }

        @Test
        @DisplayName("validation error — blank regNumber returns 400")
        void update_blankRegNumber_returns400() throws Exception {
            String json = heliJson("", "Mi-8", null, "5", "500", "ACTIVE", "2027-06-01", "400");

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem("Registration number is required")));
        }
    }

    // ==================== DELETE ====================

    @Nested
    @DisplayName("DELETE /api/v1/helicopters/{id}")
    class DeleteTests {

        @Test
        @DisplayName("happy path — returns 204")
        void delete_returns204() throws Exception {
            doNothing().when(helicopterService).delete(1L);

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("not found — returns 404")
        void delete_notFound_returns404() throws Exception {
            doThrow(new EntityNotFoundException("Helicopter", 999L))
                    .when(helicopterService).delete(999L);

            mockMvc.perform(delete(BASE_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Helicopter with id 999 not found"));
        }
    }

    // ==================== PARAMETERIZED TEST DATA ====================

    static Stream<Arguments> createValidationCases() {
        return Stream.of(
                Arguments.of("blank regNumber",
                        heliJson("", "Mi-8", null, "5", "500", "ACTIVE", "2027-06-01", "400"),
                        "Registration number is required"),
                Arguments.of("regNumber too long",
                        heliJson("R".repeat(31), "Mi-8", null, "5", "500", "ACTIVE", "2027-06-01", "400"),
                        "Registration number must be at most 30 characters"),
                Arguments.of("blank type",
                        heliJson("SP-ABC", "", null, "5", "500", "ACTIVE", "2027-06-01", "400"),
                        "Type is required"),
                Arguments.of("type too long",
                        heliJson("SP-ABC", "T".repeat(101), null, "5", "500", "ACTIVE", "2027-06-01", "400"),
                        "Type must be at most 100 characters"),
                Arguments.of("description too long",
                        heliJson("SP-ABC", "Mi-8", "D".repeat(101), "5", "500", "ACTIVE", "2027-06-01", "400"),
                        "Description must be at most 100 characters"),
                Arguments.of("null maxCrew",
                        heliJson("SP-ABC", "Mi-8", null, "null", "500", "ACTIVE", "2027-06-01", "400"),
                        "Max crew is required"),
                Arguments.of("maxCrew below 1",
                        heliJson("SP-ABC", "Mi-8", null, "0", "500", "ACTIVE", "2027-06-01", "400"),
                        "Max crew must be between 1 and 10"),
                Arguments.of("maxCrew above 10",
                        heliJson("SP-ABC", "Mi-8", null, "11", "500", "ACTIVE", "2027-06-01", "400"),
                        "Max crew must be between 1 and 10"),
                Arguments.of("null maxPayload",
                        heliJson("SP-ABC", "Mi-8", null, "5", "null", "ACTIVE", "2027-06-01", "400"),
                        "Max payload is required"),
                Arguments.of("maxPayload below 1",
                        heliJson("SP-ABC", "Mi-8", null, "5", "0", "ACTIVE", "2027-06-01", "400"),
                        "Max payload must be between 1 and 1000 kg"),
                Arguments.of("maxPayload above 1000",
                        heliJson("SP-ABC", "Mi-8", null, "5", "1001", "ACTIVE", "2027-06-01", "400"),
                        "Max payload must be between 1 and 1000 kg"),
                Arguments.of("null status",
                        heliJson("SP-ABC", "Mi-8", null, "5", "500", null, "2027-06-01", "400"),
                        "Status is required"),
                Arguments.of("null rangeKm",
                        heliJson("SP-ABC", "Mi-8", null, "5", "500", "ACTIVE", "2027-06-01", "null"),
                        "Range in km is required"),
                Arguments.of("rangeKm below 1",
                        heliJson("SP-ABC", "Mi-8", null, "5", "500", "ACTIVE", "2027-06-01", "0"),
                        "Range must be between 1 and 1000 km"),
                Arguments.of("rangeKm above 1000",
                        heliJson("SP-ABC", "Mi-8", null, "5", "500", "ACTIVE", "2027-06-01", "1001"),
                        "Range must be between 1 and 1000 km")
        );
    }
}

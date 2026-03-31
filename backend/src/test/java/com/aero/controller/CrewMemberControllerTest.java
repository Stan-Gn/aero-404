package com.aero.controller;

import com.aero.config.SecConf;
import com.aero.domain.CrewRole;
import com.aero.dto.CrewMemberResponseDto;
import com.aero.exception.EntityNotFoundException;
import com.aero.exception.GlobalExceptionHandler;
import com.aero.exception.ValidationException;
import com.aero.security.JwtTokenProvider;
import com.aero.service.CrewMemberService;
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

@WebMvcTest(CrewMemberController.class)
@Import({SecConf.class, GlobalExceptionHandler.class})
@WithMockUser(roles = "ADMIN")
class CrewMemberControllerTest {

    private static final String BASE_URL = "/api/v1/crew-members";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrewMemberService crewMemberService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // --- Helpers ---

    private static String crewJson(String firstName, String lastName, String email,
                                   String weight, String role,
                                   String licenseNumber, String licenseExpiry,
                                   String trainingExpiry) {
        return """
                {
                    "firstName": %s,
                    "lastName": %s,
                    "email": %s,
                    "weight": %s,
                    "role": %s,
                    "licenseNumber": %s,
                    "licenseExpiry": %s,
                    "trainingExpiry": %s
                }
                """.formatted(
                str(firstName), str(lastName), str(email),
                weight, str(role),
                str(licenseNumber), str(licenseExpiry), str(trainingExpiry)
        );
    }

    private static String str(String val) {
        return val == null ? "null" : "\"" + val + "\"";
    }

    private static final String VALID_PILOT_JSON = crewJson(
            "Jan", "Kowalski", "jan@test.pl", "80", "PILOT",
            "LIC-001", "2027-06-01", "2027-12-01");

    private static final String VALID_OBSERVER_JSON = crewJson(
            "Anna", "Nowak", "anna@test.pl", "65", "OBSERVER",
            null, null, "2027-12-01");

    private static CrewMemberResponseDto samplePilotResponse(Long id) {
        return new CrewMemberResponseDto(id, "Jan", "Kowalski", "jan@test.pl",
                80, CrewRole.PILOT, "LIC-001",
                LocalDate.of(2027, 6, 1), LocalDate.of(2027, 12, 1));
    }

    private static CrewMemberResponseDto sampleObserverResponse(Long id) {
        return new CrewMemberResponseDto(id, "Anna", "Nowak", "anna@test.pl",
                65, CrewRole.OBSERVER, null, null, LocalDate.of(2027, 12, 1));
    }

    // ==================== GET ALL ====================

    @Nested
    @DisplayName("GET /api/v1/crew-members")
    class GetAllTests {

        @Test
        @DisplayName("returns 200 with list")
        void getAll_returns200() throws Exception {
            when(crewMemberService.findAll()).thenReturn(List.of(
                    sampleObserverResponse(1L), samplePilotResponse(2L)));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[1].role").value("PILOT"));
        }

        @Test
        @DisplayName("returns 200 with empty list")
        void getAll_empty_returns200() throws Exception {
            when(crewMemberService.findAll()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ==================== GET BY ID ====================

    @Nested
    @DisplayName("GET /api/v1/crew-members/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("happy path — returns 200")
        void getById_returns200() throws Exception {
            when(crewMemberService.findById(1L)).thenReturn(samplePilotResponse(1L));

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.firstName").value("Jan"))
                    .andExpect(jsonPath("$.role").value("PILOT"))
                    .andExpect(jsonPath("$.licenseNumber").value("LIC-001"));
        }

        @Test
        @DisplayName("not found — returns 404")
        void getById_notFound_returns404() throws Exception {
            when(crewMemberService.findById(999L))
                    .thenThrow(new EntityNotFoundException("CrewMember", 999L));

            mockMvc.perform(get(BASE_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("CrewMember with id 999 not found"));
        }
    }

    // ==================== CREATE ====================

    @Nested
    @DisplayName("POST /api/v1/crew-members")
    class CreateTests {

        @Test
        @DisplayName("happy path — PILOT with license returns 201")
        void create_pilot_returns201() throws Exception {
            when(crewMemberService.create(any())).thenReturn(samplePilotResponse(1L));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_PILOT_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.role").value("PILOT"))
                    .andExpect(jsonPath("$.licenseNumber").value("LIC-001"));
        }

        @Test
        @DisplayName("happy path — OBSERVER without license returns 201")
        void create_observer_returns201() throws Exception {
            when(crewMemberService.create(any())).thenReturn(sampleObserverResponse(1L));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_OBSERVER_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.role").value("OBSERVER"))
                    .andExpect(jsonPath("$.licenseNumber").isEmpty());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.aero.controller.CrewMemberControllerTest#createValidationCases")
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
        @DisplayName("duplicate email — service throws ValidationException → 400")
        void create_duplicateEmail_returns400() throws Exception {
            when(crewMemberService.create(any()))
                    .thenThrow(new ValidationException("Email already exists"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_PILOT_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Email already exists"));
        }

        @Test
        @DisplayName("PILOT without license number — service throws ValidationException → 400")
        void create_pilotWithoutLicense_returns400() throws Exception {
            when(crewMemberService.create(any()))
                    .thenThrow(new ValidationException("License number is required for role PILOT"));

            String json = crewJson("Jan", "Kowalski", "jan@test.pl", "80", "PILOT",
                    null, null, "2027-12-01");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("License number is required for role PILOT"));
        }

        @Test
        @DisplayName("PILOT without license expiry — service throws ValidationException → 400")
        void create_pilotWithoutLicenseExpiry_returns400() throws Exception {
            when(crewMemberService.create(any()))
                    .thenThrow(new ValidationException("License expiry date is required for role PILOT"));

            String json = crewJson("Jan", "Kowalski", "jan@test.pl", "80", "PILOT",
                    "LIC-001", null, "2027-12-01");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("License expiry date is required for role PILOT"));
        }
    }

    // ==================== UPDATE ====================

    @Nested
    @DisplayName("PUT /api/v1/crew-members/{id}")
    class UpdateTests {

        @Test
        @DisplayName("happy path — returns 200")
        void update_returns200() throws Exception {
            when(crewMemberService.update(eq(1L), any())).thenReturn(samplePilotResponse(1L));

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_PILOT_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.firstName").value("Jan"));
        }

        @Test
        @DisplayName("not found — returns 404")
        void update_notFound_returns404() throws Exception {
            when(crewMemberService.update(eq(999L), any()))
                    .thenThrow(new EntityNotFoundException("CrewMember", 999L));

            mockMvc.perform(put(BASE_URL + "/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_PILOT_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("CrewMember with id 999 not found"));
        }

        @Test
        @DisplayName("duplicate email on update — returns 400")
        void update_duplicateEmail_returns400() throws Exception {
            when(crewMemberService.update(eq(1L), any()))
                    .thenThrow(new ValidationException("Email already exists"));

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_PILOT_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Email already exists"));
        }

        @Test
        @DisplayName("validation error — blank firstName returns 400")
        void update_blankFirstName_returns400() throws Exception {
            String json = crewJson("", "Kowalski", "jan@test.pl", "80", "PILOT",
                    "LIC-001", "2027-06-01", "2027-12-01");

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem("First name is required")));
        }
    }

    // ==================== DELETE ====================

    @Nested
    @DisplayName("DELETE /api/v1/crew-members/{id}")
    class DeleteTests {

        @Test
        @DisplayName("happy path — returns 204")
        void delete_returns204() throws Exception {
            doNothing().when(crewMemberService).delete(1L);

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("not found — returns 404")
        void delete_notFound_returns404() throws Exception {
            doThrow(new EntityNotFoundException("CrewMember", 999L))
                    .when(crewMemberService).delete(999L);

            mockMvc.perform(delete(BASE_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("CrewMember with id 999 not found"));
        }
    }

    // ==================== PARAMETERIZED TEST DATA ====================

    static Stream<Arguments> createValidationCases() {
        return Stream.of(
                Arguments.of("blank firstName",
                        crewJson("", "Kowalski", "jan@test.pl", "80", "OBSERVER", null, null, "2027-12-01"),
                        "First name is required"),
                Arguments.of("firstName too long",
                        crewJson("A".repeat(101), "Kowalski", "jan@test.pl", "80", "OBSERVER", null, null, "2027-12-01"),
                        "First name must be at most 100 characters"),
                Arguments.of("blank lastName",
                        crewJson("Jan", "", "jan@test.pl", "80", "OBSERVER", null, null, "2027-12-01"),
                        "Last name is required"),
                Arguments.of("lastName too long",
                        crewJson("Jan", "K".repeat(101), "jan@test.pl", "80", "OBSERVER", null, null, "2027-12-01"),
                        "Last name must be at most 100 characters"),
                Arguments.of("blank email",
                        crewJson("Jan", "Kowalski", "", "80", "OBSERVER", null, null, "2027-12-01"),
                        "Email is required"),
                Arguments.of("invalid email",
                        crewJson("Jan", "Kowalski", "not-an-email", "80", "OBSERVER", null, null, "2027-12-01"),
                        "Email must be valid"),
                Arguments.of("null weight",
                        crewJson("Jan", "Kowalski", "jan@test.pl", "null", "OBSERVER", null, null, "2027-12-01"),
                        "Weight is required"),
                Arguments.of("weight below 30",
                        crewJson("Jan", "Kowalski", "jan@test.pl", "29", "OBSERVER", null, null, "2027-12-01"),
                        "Weight must be between 30 and 200 kg"),
                Arguments.of("weight above 200",
                        crewJson("Jan", "Kowalski", "jan@test.pl", "201", "OBSERVER", null, null, "2027-12-01"),
                        "Weight must be between 30 and 200 kg"),
                Arguments.of("null role",
                        crewJson("Jan", "Kowalski", "jan@test.pl", "80", null, null, null, "2027-12-01"),
                        "Role is required"),
                Arguments.of("null trainingExpiry",
                        crewJson("Jan", "Kowalski", "jan@test.pl", "80", "OBSERVER", null, null, null),
                        "Training expiry date is required"),
                Arguments.of("licenseNumber too long",
                        crewJson("Jan", "Kowalski", "jan@test.pl", "80", "OBSERVER",
                                "L".repeat(31), null, "2027-12-01"),
                        "License number must be at most 30 characters")
        );
    }
}

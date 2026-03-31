package com.aero.controller;

import com.aero.config.SecConf;
import com.aero.domain.UserRole;
import com.aero.dto.LoginResponseDto;
import com.aero.dto.RegisterResponseDto;
import com.aero.exception.GlobalExceptionHandler;
import com.aero.exception.ValidationException;
import com.aero.security.JwtTokenProvider;
import com.aero.service.AuthService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecConf.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // --- Helper ---

    private static String registerJson(String firstName, String lastName, String email, String password) {
        return """
                {
                    "firstName": %s,
                    "lastName": %s,
                    "email": %s,
                    "password": %s
                }
                """.formatted(
                firstName == null ? "null" : "\"" + firstName + "\"",
                lastName == null ? "null" : "\"" + lastName + "\"",
                email == null ? "null" : "\"" + email + "\"",
                password == null ? "null" : "\"" + password + "\""
        );
    }

    private static String loginJson(String email, String password) {
        return """
                {
                    "email": %s,
                    "password": %s
                }
                """.formatted(
                email == null ? "null" : "\"" + email + "\"",
                password == null ? "null" : "\"" + password + "\""
        );
    }

    // ==================== REGISTER ====================

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("happy path — valid data returns 201 with body")
        void register_withValidData_returns201() throws Exception {
            when(authService.register(any()))
                    .thenReturn(new RegisterResponseDto(1L, "jan@test.pl", UserRole.PLANNER));

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson("Jan", "Kowalski", "jan@test.pl", "password123")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("jan@test.pl"))
                    .andExpect(jsonPath("$.role").value("PLANNER"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.aero.controller.AuthControllerTest#registerValidationCases")
        @DisplayName("validation error returns 400 with expected message")
        void register_withInvalidField_returns400(String testName, String json, String expectedError) throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors", hasItem(expectedError)));
        }

        @Test
        @DisplayName("all fields null — returns 400 with multiple errors")
        void register_withNullFields_returns400WithMultipleErrors() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(4))));
        }

        @Test
        @DisplayName("duplicate email — service throws ValidationException → 400")
        void register_withDuplicateEmail_returns400() throws Exception {
            when(authService.register(any()))
                    .thenThrow(new ValidationException("Email already exists"));

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson("Jan", "Kowalski", "jan@test.pl", "password123")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Email already exists"));
        }

        @Test
        @DisplayName("wrong content type — returns error (not 2xx)")
        void register_withWrongContentType_returnsError() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("some text"))
                    .andExpect(status().is5xxServerError());
        }
    }

    // ==================== LOGIN ====================

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("happy path — valid credentials returns 200 with token")
        void login_withValidCredentials_returns200() throws Exception {
            when(authService.login(any()))
                    .thenReturn(new LoginResponseDto("jwt-token-xyz", "pilot@test.pl", UserRole.PILOT));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("pilot@test.pl", "password123")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token-xyz"))
                    .andExpect(jsonPath("$.email").value("pilot@test.pl"))
                    .andExpect(jsonPath("$.role").value("PILOT"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.aero.controller.AuthControllerTest#loginValidationCases")
        @DisplayName("validation error returns 400 with expected message")
        void login_withInvalidField_returns400(String testName, String json, String expectedError) throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors", hasItem(expectedError)));
        }

        @Test
        @DisplayName("wrong password — service throws ValidationException → 400")
        void login_withWrongPassword_returns400() throws Exception {
            when(authService.login(any()))
                    .thenThrow(new ValidationException("Invalid credentials"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("pilot@test.pl", "wrongpassword")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid credentials"));
        }

        @Test
        @DisplayName("nonexistent email — service throws ValidationException → 400")
        void login_withNonexistentEmail_returns400() throws Exception {
            when(authService.login(any()))
                    .thenThrow(new ValidationException("Invalid credentials"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("nobody@test.pl", "password123")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid credentials"));
        }

        @Test
        @DisplayName("no role assigned — service throws ValidationException → 400")
        void login_withNoRoleAssigned_returns400() throws Exception {
            when(authService.login(any()))
                    .thenThrow(new ValidationException("Account has no role assigned. Contact administrator."));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("norole@test.pl", "password123")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Account has no role assigned. Contact administrator."));
        }

        @Test
        @DisplayName("GET method not allowed — returns error (not 2xx)")
        void login_withGetMethod_returnsError() throws Exception {
            mockMvc.perform(get(LOGIN_URL))
                    .andExpect(status().is5xxServerError());
        }
    }

    // ==================== PARAMETERIZED TEST DATA ====================

    static Stream<Arguments> registerValidationCases() {
        return Stream.of(
                Arguments.of("blank firstName",
                        registerJson("", "Kowalski", "jan@test.pl", "password123"),
                        "First name is required"),
                Arguments.of("blank lastName",
                        registerJson("Jan", "", "jan@test.pl", "password123"),
                        "Last name is required"),
                Arguments.of("blank email",
                        registerJson("Jan", "Kowalski", "", "password123"),
                        "Email is required"),
                Arguments.of("invalid email format",
                        registerJson("Jan", "Kowalski", "not-an-email", "password123"),
                        "Email must be valid"),
                Arguments.of("blank password",
                        registerJson("Jan", "Kowalski", "jan@test.pl", ""),
                        "Password is required"),
                Arguments.of("password too short",
                        registerJson("Jan", "Kowalski", "jan@test.pl", "abc"),
                        "Password must be at least 8 characters")
        );
    }

    static Stream<Arguments> loginValidationCases() {
        return Stream.of(
                Arguments.of("blank email",
                        loginJson("", "password123"),
                        "Email is required"),
                Arguments.of("invalid email format",
                        loginJson("bad-email", "password123"),
                        "Email must be valid"),
                Arguments.of("blank password",
                        loginJson("pilot@test.pl", ""),
                        "Password is required")
        );
    }
}

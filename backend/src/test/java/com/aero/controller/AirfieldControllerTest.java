package com.aero.controller;

import com.aero.config.SecConf;
import com.aero.dto.AirfieldResponseDto;
import com.aero.exception.EntityNotFoundException;
import com.aero.exception.GlobalExceptionHandler;
import com.aero.security.JwtTokenProvider;
import com.aero.service.AirfieldService;
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

@WebMvcTest(AirfieldController.class)
@Import({SecConf.class, GlobalExceptionHandler.class})
@WithMockUser(roles = "ADMIN")
class AirfieldControllerTest {

    private static final String BASE_URL = "/api/v1/airfields";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AirfieldService airfieldService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // --- Helper ---

    private static String airfieldJson(String name, String latitude, String longitude) {
        return """
                {
                    "name": %s,
                    "latitude": %s,
                    "longitude": %s
                }
                """.formatted(
                name == null ? "null" : "\"" + name + "\"",
                latitude,
                longitude
        );
    }

    private static final String VALID_JSON = airfieldJson("Lotnisko Wrocław", "51.1", "17.0");

    private static AirfieldResponseDto sampleResponse(Long id) {
        return new AirfieldResponseDto(id, "Lotnisko Wrocław", 51.1, 17.0);
    }

    // ==================== GET ALL ====================

    @Nested
    @DisplayName("GET /api/v1/airfields")
    class GetAllTests {

        @Test
        @DisplayName("returns 200 with list of airfields")
        void getAll_returns200WithList() throws Exception {
            when(airfieldService.findAll()).thenReturn(List.of(
                    new AirfieldResponseDto(1L, "Lotnisko A", 51.1, 17.0),
                    new AirfieldResponseDto(2L, "Lotnisko B", 52.2, 18.0)
            ));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Lotnisko A"))
                    .andExpect(jsonPath("$[1].id").value(2));
        }

        @Test
        @DisplayName("returns 200 with empty list when no airfields")
        void getAll_returnsEmptyList() throws Exception {
            when(airfieldService.findAll()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ==================== GET BY ID ====================

    @Nested
    @DisplayName("GET /api/v1/airfields/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("happy path — returns 200 with airfield")
        void getById_returns200() throws Exception {
            when(airfieldService.findById(1L)).thenReturn(sampleResponse(1L));

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Lotnisko Wrocław"))
                    .andExpect(jsonPath("$.latitude").value(51.1))
                    .andExpect(jsonPath("$.longitude").value(17.0));
        }

        @Test
        @DisplayName("not found — returns 404")
        void getById_notFound_returns404() throws Exception {
            when(airfieldService.findById(999L))
                    .thenThrow(new EntityNotFoundException("Airfield", 999L));

            mockMvc.perform(get(BASE_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Airfield with id 999 not found"));
        }
    }

    // ==================== CREATE ====================

    @Nested
    @DisplayName("POST /api/v1/airfields")
    class CreateTests {

        @Test
        @DisplayName("happy path — valid data returns 201")
        void create_withValidData_returns201() throws Exception {
            when(airfieldService.create(any())).thenReturn(sampleResponse(1L));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Lotnisko Wrocław"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.aero.controller.AirfieldControllerTest#createValidationCases")
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
                    .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(3))));
        }
    }

    // ==================== UPDATE ====================

    @Nested
    @DisplayName("PUT /api/v1/airfields/{id}")
    class UpdateTests {

        @Test
        @DisplayName("happy path — valid data returns 200")
        void update_withValidData_returns200() throws Exception {
            when(airfieldService.update(eq(1L), any())).thenReturn(sampleResponse(1L));

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Lotnisko Wrocław"));
        }

        @Test
        @DisplayName("not found — returns 404")
        void update_notFound_returns404() throws Exception {
            when(airfieldService.update(eq(999L), any()))
                    .thenThrow(new EntityNotFoundException("Airfield", 999L));

            mockMvc.perform(put(BASE_URL + "/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Airfield with id 999 not found"));
        }

        @Test
        @DisplayName("validation error — blank name returns 400")
        void update_withBlankName_returns400() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(airfieldJson("", "51.1", "17.0")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem("Name is required")));
        }
    }

    // ==================== DELETE ====================

    @Nested
    @DisplayName("DELETE /api/v1/airfields/{id}")
    class DeleteTests {

        @Test
        @DisplayName("happy path — returns 204 no content")
        void delete_returns204() throws Exception {
            doNothing().when(airfieldService).delete(1L);

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("not found — returns 404")
        void delete_notFound_returns404() throws Exception {
            doThrow(new EntityNotFoundException("Airfield", 999L))
                    .when(airfieldService).delete(999L);

            mockMvc.perform(delete(BASE_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Airfield with id 999 not found"));
        }
    }

    // ==================== PARAMETERIZED TEST DATA ====================

    static Stream<Arguments> createValidationCases() {
        return Stream.of(
                Arguments.of("blank name",
                        airfieldJson("", "51.1", "17.0"),
                        "Name is required"),
                Arguments.of("name too long (201 chars)",
                        airfieldJson("A".repeat(201), "51.1", "17.0"),
                        "Name must be at most 200 characters"),
                Arguments.of("null latitude",
                        airfieldJson("Lotnisko", "null", "17.0"),
                        "Latitude is required"),
                Arguments.of("latitude below -90",
                        airfieldJson("Lotnisko", "-91.0", "17.0"),
                        "Latitude must be between -90 and 90"),
                Arguments.of("latitude above 90",
                        airfieldJson("Lotnisko", "91.0", "17.0"),
                        "Latitude must be between -90 and 90"),
                Arguments.of("null longitude",
                        airfieldJson("Lotnisko", "51.1", "null"),
                        "Longitude is required"),
                Arguments.of("longitude below -180",
                        airfieldJson("Lotnisko", "51.1", "-181.0"),
                        "Longitude must be between -180 and 180"),
                Arguments.of("longitude above 180",
                        airfieldJson("Lotnisko", "51.1", "181.0"),
                        "Longitude must be between -180 and 180")
        );
    }
}

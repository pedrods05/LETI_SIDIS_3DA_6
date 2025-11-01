package leti_sisdis_6.happatients.exceptions;

import leti_sisdis_6.happatients.api.PatientRegistrationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PatientRegistrationController.class)
@Import({GlobalExceptionHandlerTest.MockBeans.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @TestConfiguration
    static class MockBeans {
        @Bean JwtDecoder jwtDecoder() { return mock(JwtDecoder.class); }
        @Bean leti_sisdis_6.happatients.service.PatientRegistrationService registrationService() {
            return mock(leti_sisdis_6.happatients.service.PatientRegistrationService.class);
        }
    }

    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void setup() {}

    @Test
    void methodArgumentNotValid_returns400_withDetails() throws Exception {
        // body inválido: falta de campos obrigatórios no DTO v2
        String invalidBody = "{}";
        mockMvc.perform(post("/api/v2/patients/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isUnauthorized());
    }
}

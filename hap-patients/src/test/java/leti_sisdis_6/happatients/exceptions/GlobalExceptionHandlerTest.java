package leti_sisdis_6.happatients.exceptions;

import leti_sisdis_6.happatients.api.PatientRegistrationController;
import leti_sisdis_6.happatients.service.PatientRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WebMvcTest(controllers = PatientRegistrationController.class)
@ContextConfiguration(classes = {
        GlobalExceptionHandlerTest.MinimalApp.class, // A nossa config mínima
        PatientRegistrationController.class,         // O Controller que queremos testar <--- ADICIONA ESTE
        GlobalExceptionHandler.class                 // O Handler de erros
})
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @SpringBootApplication(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class,
            SecurityAutoConfiguration.class
    })
    static class MinimalApp {
        @Bean
        public PatientRegistrationService patientRegistrationService() {
            return mock(PatientRegistrationService.class);
        }

        @Bean
        public JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }

    @Autowired private MockMvc mockMvc;

    @Test
    void methodArgumentNotValid_returns400_withDetails() throws Exception {
        String invalidBody = "{}";
        mockMvc.perform(post("/api/v2/patients/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }
}
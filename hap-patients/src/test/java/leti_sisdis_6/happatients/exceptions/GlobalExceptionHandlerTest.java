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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 1. Inicializa o contexto Web
@WebMvcTest(controllers = PatientRegistrationController.class)
// 2. Define a configuração exata (MinimalApp + Controller + Handler)
@ContextConfiguration(classes = {
        GlobalExceptionHandlerTest.MinimalApp.class,
        PatientRegistrationController.class,
        GlobalExceptionHandler.class
})
// 3. Desliga a segurança para testar apenas a validação do JSON (evita 401/403)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    // 4. Configuração isolada para não tentar ligar a BDs reais
    @SpringBootApplication(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class
    })
    static class MinimalApp {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private PatientRegistrationService registrationService;

    @Test
    void methodArgumentNotValid_returns400_withDetails() throws Exception {
        // body inválido (vazio)
        String invalidBody = "{}";

        mockMvc.perform(post("/api/v2/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest()); // Agora esperamos 400 (Bad Request)
    }
}
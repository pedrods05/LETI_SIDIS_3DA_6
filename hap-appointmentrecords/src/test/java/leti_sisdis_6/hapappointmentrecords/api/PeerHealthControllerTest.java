package leti_sisdis_6.hapappointmentrecords.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRecordRepository;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PeerHealthController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "hap.physicians.base-url=http://localhost:8081",
    "hap.patients.base-url=http://localhost:8082",
    "hap.auth.base-url=http://localhost:8084"
})
class PeerHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @MockitoBean
    private AppointmentRecordRepository appointmentRecordRepository;

    @MockitoBean
    private RestTemplate restTemplate;

    @Test
    @DisplayName("GET /api/peers/health → marca UP quando algum probe responde")
    void health_upWhenAnyProbeResponds() throws Exception {
        // Configurar mock para retornar sucesso em qualquer chamada
        given(restTemplate.getForEntity(anyString(), eq(String.class)))
                .willReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(get("/api/peers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name", is("physicians")))
                .andExpect(jsonPath("$[0].status", is("UP")))
                .andExpect(jsonPath("$[0].baseUrl", is("http://localhost:8081")))
                .andExpect(jsonPath("$[0].probe", notNullValue()))
                .andExpect(jsonPath("$[1].name", is("patients")))
                .andExpect(jsonPath("$[1].status", is("UP")))
                .andExpect(jsonPath("$[1].baseUrl", is("http://localhost:8082")))
                .andExpect(jsonPath("$[2].name", is("auth")))
                .andExpect(jsonPath("$[2].status", is("UP")))
                .andExpect(jsonPath("$[2].baseUrl", is("http://localhost:8084")));
    }

    @Test
    @DisplayName("GET /api/peers/health → marca DOWN quando todos os probes falham")
    void health_downWhenAllFail() throws Exception {
        // Configurar mock para lançar exceção em qualquer chamada
        given(restTemplate.getForEntity(anyString(), eq(String.class)))
                .willThrow(new RuntimeException("Connection failed"));

        mockMvc.perform(get("/api/peers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name", is("physicians")))
                .andExpect(jsonPath("$[0].status", is("DOWN")))
                .andExpect(jsonPath("$[0].baseUrl", is("http://localhost:8081")))
                .andExpect(jsonPath("$[0]", not(hasKey("probe"))))
                .andExpect(jsonPath("$[1].name", is("patients")))
                .andExpect(jsonPath("$[1].status", is("DOWN")))
                .andExpect(jsonPath("$[1].baseUrl", is("http://localhost:8082")))
                .andExpect(jsonPath("$[2].name", is("auth")))
                .andExpect(jsonPath("$[2].status", is("DOWN")))
                .andExpect(jsonPath("$[2].baseUrl", is("http://localhost:8084")));
    }

    @Test
    @DisplayName("GET /api/peers/health → retorna estrutura correta")
    void health_correctStructure() throws Exception {
        given(restTemplate.getForEntity(anyString(), eq(String.class)))
                .willReturn(ResponseEntity.ok("service is up"));

        mockMvc.perform(get("/api/peers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0]", allOf(
                    hasKey("name"),
                    hasKey("baseUrl"),
                    hasKey("status"),
                    hasKey("probe")
                )))
                .andExpect(jsonPath("$[1]", allOf(
                    hasKey("name"),
                    hasKey("baseUrl"),
                    hasKey("status"),
                    hasKey("probe")
                )))
                .andExpect(jsonPath("$[2]", allOf(
                    hasKey("name"),
                    hasKey("baseUrl"),
                    hasKey("status"),
                    hasKey("probe")
                )));
    }
}

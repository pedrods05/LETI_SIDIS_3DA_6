package leti_sisdis_6.hapappointmentrecords.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PeerHealthController.class)
@AutoConfigureMockMvc(addFilters = false)
class PeerHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    @DisplayName("GET /api/peers/health → marca UP quando algum probe responde")
    void health_upWhenAnyProbeResponds() throws Exception {
        // Any call to getForEntity returns 200 OK
        given(restTemplate.getForEntity(anyString(), eq(String.class)))
                .willReturn(ResponseEntity.ok("ok"));

        mockMvc.perform(get("/api/peers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].status", everyItem(is("UP"))));
    }

    @Test
    @DisplayName("GET /api/peers/health → marca DOWN quando todos os probes falham")
    void health_downWhenAllFail() throws Exception {
        given(restTemplate.getForEntity(anyString(), eq(String.class)))
                .willThrow(new RuntimeException("fail"));

        mockMvc.perform(get("/api/peers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].status", everyItem(is("DOWN"))));
    }
}

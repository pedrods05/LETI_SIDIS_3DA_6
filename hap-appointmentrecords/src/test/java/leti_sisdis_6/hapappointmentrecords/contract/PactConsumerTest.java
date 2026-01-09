package leti_sisdis_6.hapappointmentrecords.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "hap-patients-service")
public class PactConsumerTest {

    @Pact(consumer = "hap-appointmentrecords-service")
    public V4Pact createPact(PactBuilder builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        return builder
                .usingLegacyDsl()
                .given("patient with ID 1 exists")
                .uponReceiving("A request for patient details")
                .path("/patients/1")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body("{\"patientId\": \"1\", \"fullName\": \"John Doe\", \"email\": \"john@example.com\"}")
                .toPact(V4Pact.class);
    }

    @Test
    void testPatientDetailsContract(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        String url = mockServer.getUrl() + "/patients/1";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("patientId");
        assertThat(response.getBody()).contains("fullName");
        assertThat(response.getBody()).contains("John Doe");
    }
}
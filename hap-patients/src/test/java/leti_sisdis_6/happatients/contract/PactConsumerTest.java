package leti_sisdis_6.happatients.contract;

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
@PactTestFor(providerName = "hap-physicians-service")
public class PactConsumerTest {

    @Pact(consumer = "hap-patients-service")
    public V4Pact createPhysicianPact(PactBuilder builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        return builder
                .usingLegacyDsl()
                .given("physician with ID 1 exists")
                .uponReceiving("A request for physician details")
                .path("/physicians/1")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body("{\"physicianId\": \"1\", \"fullName\": \"Dr. John Smith\", \"licenseNumber\": \"LIC12345\", \"username\": \"dr.smith@example.com\"}")
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "hap-patients-service")
    public V4Pact createAppointmentPact(PactBuilder builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        return builder
                .usingLegacyDsl()
                .given("appointment with ID 1 exists")
                .uponReceiving("A request for appointment details")
                .path("/appointments/1")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body("{\"appointmentId\": \"1\", \"patientId\": \"P001\", \"patientName\": \"Jane Doe\", \"status\": \"SCHEDULED\"}")
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createPhysicianPact")
    void testPhysicianDetailsContract(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        String url = mockServer.getUrl() + "/physicians/1";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("physicianId");
        assertThat(response.getBody()).contains("fullName");
        assertThat(response.getBody()).contains("Dr. John Smith");
    }

    @Test
    @PactTestFor(pactMethod = "createAppointmentPact")
    void testAppointmentDetailsContract(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        String url = mockServer.getUrl() + "/appointments/1";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("appointmentId");
        assertThat(response.getBody()).contains("patientId");
        assertThat(response.getBody()).contains("SCHEDULED");
    }
}

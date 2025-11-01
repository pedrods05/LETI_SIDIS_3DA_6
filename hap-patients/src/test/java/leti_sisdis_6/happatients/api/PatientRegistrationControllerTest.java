package leti_sisdis_6.happatients.api;

import leti_sisdis_6.happatients.dto.PatientRegistrationDTOV2;
import leti_sisdis_6.happatients.exceptions.EmailAlreadyExistsException;
import leti_sisdis_6.happatients.service.PatientRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PatientRegistrationControllerTest {

    private MockMvc mockMvc;

    @Mock private PatientRegistrationService registrationService;
    @InjectMocks private PatientRegistrationController controller;

    @BeforeEach
    void setup() { this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build(); }

    @Test
    void register_ok_returns201() throws Exception {
        when(registrationService.registerPatient(any(PatientRegistrationDTOV2.class))).thenReturn("PAT03");
        String body = "{\n" +
                "  \"fullName\": \"John Doe\",\n" +
                "  \"email\": \"john@e.com\",\n" +
                "  \"password\": \"ValidPass#123\",\n" +
                "  \"phoneNumber\": \"+351912345678\",\n" +
                "  \"birthDate\": \"2000-01-01\",\n" +
                "  \"address\": {\n" +
                "    \"street\": \"A\", \"city\": \"B\", \"postalCode\": \"C\", \"country\": \"D\"\n" +
                "  },\n" +
                "  \"insuranceInfo\": {\n" +
                "    \"policyNumber\": \"P\", \"companyName\": \"I\", \"coverageType\": \"Basic\"\n" +
                "  },\n" +
                "  \"photo\": {\n" +
                "    \"url\": \"u\", \"uploadedAt\": \"2024-01-01T00:00:00\"\n" +
                "  },\n" +
                "  \"dataConsentGiven\": true\n" +
                "}";

        mockMvc.perform(post("/api/v2/patients/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value("PAT03"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void register_conflict_returns409() throws Exception {
        when(registrationService.registerPatient(any(PatientRegistrationDTOV2.class)))
                .thenThrow(new EmailAlreadyExistsException("exists"));

        String body = "{\n" +
                "  \"fullName\": \"John Doe\",\n" +
                "  \"email\": \"john@e.com\",\n" +
                "  \"password\": \"ValidPass#123\",\n" +
                "  \"phoneNumber\": \"+351912345678\",\n" +
                "  \"birthDate\": \"2000-01-01\",\n" +
                "  \"address\": {\n" +
                "    \"street\": \"A\", \"city\": \"B\", \"postalCode\": \"C\", \"country\": \"D\"\n" +
                "  },\n" +
                "  \"insuranceInfo\": {\n" +
                "    \"policyNumber\": \"P\", \"companyName\": \"I\", \"coverageType\": \"Basic\"\n" +
                "  },\n" +
                "  \"photo\": {\n" +
                "    \"url\": \"u\", \"uploadedAt\": \"2024-01-01T00:00:00\"\n" +
                "  },\n" +
                "  \"dataConsentGiven\": true\n" +
                "}";

        mockMvc.perform(post("/api/v2/patients/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already exists"));
    }
}


package leti_sisdis_6.happatients.api;

import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import leti_sisdis_6.happatients.dto.PatientProfileDTO;
import leti_sisdis_6.happatients.http.ResilientRestTemplate;
import leti_sisdis_6.happatients.repository.PatientLocalRepository;
import leti_sisdis_6.happatients.service.PatientQueryService;
import leti_sisdis_6.happatients.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
@Import(PatientSecurityTest.MockBeans.class)
class PatientSecurityTest {

    @TestConfiguration
    static class MockBeans {
        @Bean JwtDecoder jwtDecoder() { return mock(JwtDecoder.class); }
        @Bean PatientService patientService() { return mock(PatientService.class); }
        @Bean PatientLocalRepository patientLocalRepository() { return mock(PatientLocalRepository.class); }
        @Bean ResilientRestTemplate resilientRestTemplate() { return mock(ResilientRestTemplate.class); }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private PatientService patientService;
    @Autowired private PatientLocalRepository localRepository;
    @Autowired private PatientQueryService patientQueryService; // <--- ADICIONADO

    @Test
    void listPatients_requiresAdmin() throws Exception {
        mockMvc.perform(get("/patients"))
                .andExpect(status().isUnauthorized());

        when(patientService.listAllPatients()).thenReturn(List.of(PatientDetailsDTO.builder()
                .patientId("PAT01").fullName("Alice").email("a@a").build()));

        mockMvc.perform(get("/patients")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value("PAT01"));
    }

    @Test
    void getPatientDetails_requiresAdmin() throws Exception {
        String id = "PAT01";
        mockMvc.perform(get("/patients/{id}", id))
                .andExpect(status().is4xxClientError());

        when(localRepository.findById(id)).thenReturn(Optional.of(PatientDetailsDTO.builder()
                .patientId(id).fullName("Local").email("l@l").build()));

        mockMvc.perform(get("/patients/{id}", id)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(id));
    }

    @Test
    void getPatientProfile_requiresPhysician() throws Exception {
        String id = "PAT01";
        mockMvc.perform(get("/patients/{id}/profile", id))
                .andExpect(status().isUnauthorized());

        PatientProfileDTO profile = PatientProfileDTO.builder()
                .patientId(id)
                .fullName("Alice")
                .email("a@a")
                .build();

        when(patientService.getPatientProfile(any(), any())).thenReturn(profile);

        mockMvc.perform(get("/patients/{id}/profile", id)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new SimpleGrantedAuthority("PHYSICIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(id)); // CORREÇÃO: Validar campo na raiz
    }

    @Test
    void updateMyContact_requiresPatient() throws Exception {
        String body = "{\n" +
                "  \"phoneNumber\": \"+351912345678\",\n" +
                "  \"address\": {\n" +
                "    \"street\": \"A\", \"city\": \"B\", \"postalCode\": \"C\", \"country\": \"D\"\n" +
                "  }\n" +
                "}";

        mockMvc.perform(patch("/patients/me").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        when(patientService.updateContactDetails(any(), any())).thenReturn("Contact details updated successfully.");
        mockMvc.perform(patch("/patients/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new SimpleGrantedAuthority("PATIENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contact details updated successfully."));
    }
}

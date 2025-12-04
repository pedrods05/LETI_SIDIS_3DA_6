package leti_sisdis_6.happatients.api;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import leti_sisdis_6.happatients.http.ResilientRestTemplate;
import leti_sisdis_6.happatients.repository.PatientLocalRepository;
import leti_sisdis_6.happatients.service.PatientService;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    private MockMvc mockMvc;

    @Mock private PatientService patientService;
    @Mock private PatientLocalRepository localRepository;
    @Mock private ResilientRestTemplate resilientRestTemplate;

    @InjectMocks private PatientController controller;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(controller, "patientPeersProp", "http://localhost:18080");
        ReflectionTestUtils.setField(controller, "serverPort", 0);
        ReflectionTestUtils.invokeMethod(controller, "initPeers");
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listAllPatients_returns200() throws Exception {
        when(patientService.listAllPatients()).thenReturn(List.of(
                PatientDetailsDTO.builder().patientId("PAT01").fullName("Alice").email("a@a").build()
        ));

        mockMvc.perform(get("/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value("PAT01"));
    }

    @Test
    void getPatientDetails_localCacheHit() throws Exception {
        String id = "PAT01";
        PatientDetailsDTO local = PatientDetailsDTO.builder().patientId(id).fullName("Local").email("l@l").build();
        when(localRepository.findById(id)).thenReturn(Optional.of(local));

        mockMvc.perform(get("/patients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Local"));

        verifyNoInteractions(patientService);
    }

    @Test
    void getPatientDetails_notFoundAnywhere_returns404() throws Exception {
        String id = "PAT999";
        when(localRepository.findById(id)).thenReturn(Optional.empty());
        when(patientService.getPatientDetails(id)).thenThrow(new EntityNotFoundException("not found"));
        when(resilientRestTemplate.getForObjectWithFallback(anyString(), eq(PatientDetailsDTO.class))).thenReturn(null);

        mockMvc.perform(get("/patients/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchPatients_found_returns200() throws Exception {
        when(patientService.searchPatientsByName("ali")).thenReturn(List.of(
                PatientDetailsDTO.builder().patientId("PAT01").fullName("Alice").email("a@a").build()
        ));

        mockMvc.perform(get("/patients/search").param("name", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Alice"));
    }

    @Test
    void updateContactDetails_ok_returns200() throws Exception {
        // Prepare SecurityContext with email as principal name
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("john@example.com", "pass"));
        when(patientService.updateContactDetails(eq("john@example.com"), any())).thenReturn("Contact details updated successfully.");

        String body = "{\n" +
                "  \"phoneNumber\": \"999\",\n" +
                "  \"address\": {\n" +
                "    \"street\": \"A\", \"city\": \"B\", \"postalCode\": \"C\", \"country\": \"D\"\n" +
                "  }\n" +
                "}";

        mockMvc.perform(patch("/patients/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contact details updated successfully."));
    }
}


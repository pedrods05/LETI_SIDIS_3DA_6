package leti_sisdis_6.happatients.api;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import leti_sisdis_6.happatients.repository.PatientLocalRepository;
import leti_sisdis_6.happatients.service.PatientService;

@ExtendWith(MockitoExtension.class)
class InternalPatientControllerTest {

    private MockMvc mockMvc;

    @Mock private PatientService patientService;
    @Mock private PatientLocalRepository localRepository;

    @InjectMocks private InternalPatientController controller;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void returnsLocalCacheWhenAvailable() throws Exception {
        String id = "PAT01";
        PatientDetailsDTO cached = PatientDetailsDTO.builder().patientId(id).fullName("Local").email("l@l").build();
        when(localRepository.findById(id)).thenReturn(Optional.of(cached));

        mockMvc.perform(get("/internal/patients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(id))
                .andExpect(jsonPath("$.fullName").value("Local"));

        verifyNoInteractions(patientService);
    }

    @Test
    void callsServiceAndCachesWhenNotInLocal() throws Exception {
        String id = "PAT02";
        when(localRepository.findById(id)).thenReturn(Optional.empty());
        PatientDetailsDTO fromSvc = PatientDetailsDTO.builder().patientId(id).fullName("FromSvc").email("s@s").build();
        when(patientService.getPatientDetails(id)).thenReturn(fromSvc);

        mockMvc.perform(get("/internal/patients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("FromSvc"));

        verify(localRepository).save(fromSvc);
    }

    @Test
    void returns404WhenNotFound() throws Exception {
        String id = "PAT404";
        when(localRepository.findById(id)).thenReturn(Optional.empty());
        when(patientService.getPatientDetails(id)).thenThrow(new EntityNotFoundException("not found"));

        mockMvc.perform(get("/internal/patients/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Patient not found"));
    }
}


package leti_sisdis_6.happatients.api;

import jakarta.persistence.EntityNotFoundException;
import leti_sisdis_6.happatients.exceptions.NotFoundException;
import leti_sisdis_6.happatients.service.PatientQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import leti_sisdis_6.happatients.http.ResilientRestTemplate;
import leti_sisdis_6.happatients.repository.PatientLocalRepository;
import leti_sisdis_6.happatients.service.PatientService;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatientControllerPeerForwardingTest {

    private MockMvc mockMvc;

    @Mock
    private PatientService patientService;

    @Mock
    private PatientQueryService patientQueryService;

    @Mock
    private PatientLocalRepository localRepository;

    @Mock
    private ResilientRestTemplate resilientRestTemplate;

    @InjectMocks
    private PatientController controller;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(controller, "patientPeersProp", "http://localhost:18080");
        ReflectionTestUtils.setField(controller, "serverPort", 0);
        ReflectionTestUtils.invokeMethod(controller, "initPeers");

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void whenNotFoundLocally_shouldForwardToPeer_andReturnPeerResult() throws Exception {
        String id = "PAT01";

        when(patientQueryService.getPatientProfile(anyString())).thenThrow(new NotFoundException("Not found in Mongo"));

        when(localRepository.findById(id)).thenReturn(Optional.empty());

        when(patientService.getPatientDetails(id)).thenThrow(new EntityNotFoundException("not found"));

        PatientDetailsDTO remote = PatientDetailsDTO.builder()
                .patientId(id)
                .fullName("Alice Peer")
                .email("alice@peer.test")
                .build();
        when(resilientRestTemplate.getForObjectWithFallback(anyString(), eq(PatientDetailsDTO.class)))
                .thenReturn(remote);

        mockMvc.perform(get("/patients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(id))
                .andExpect(jsonPath("$.fullName").value("Alice Peer"))
                .andExpect(jsonPath("$.email").value("alice@peer.test"));

        verify(resilientRestTemplate, atLeastOnce()).getForObjectWithFallback(contains("/internal/patients/" + id), eq(PatientDetailsDTO.class));
    }

    @Test
    void whenNotFoundLocally_andPeerFails_shouldReturn404() throws Exception {
        String id = "PAT404";

        when(patientQueryService.getPatientProfile(anyString())).thenThrow(new NotFoundException("Not found"));

        when(localRepository.findById(id)).thenReturn(Optional.empty());

        when(patientService.getPatientDetails(id)).thenThrow(new EntityNotFoundException("not found"));

        when(resilientRestTemplate.getForObjectWithFallback(anyString(), eq(PatientDetailsDTO.class)))
                .thenThrow(new RuntimeException("peer down"));

        mockMvc.perform(get("/patients/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenLocalHasData_shouldReturnLocal_andNeverCallPeers() throws Exception {
        String id = "PAT_LOCAL";

        when(patientQueryService.getPatientProfile(anyString())).thenThrow(new NotFoundException("Not found"));

        PatientDetailsDTO local = PatientDetailsDTO.builder()
                .patientId(id)
                .fullName("Local User")
                .email("local@example.com")
                .build();
        when(patientService.getPatientDetails(id)).thenReturn(local);

        mockMvc.perform(get("/patients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(id))
                .andExpect(jsonPath("$.fullName").value("Local User"));

        verifyNoInteractions(resilientRestTemplate);
    }

    @Test
    void whenIdInvalid_orNotFoundAnywhere_shouldReturn404() throws Exception {
        String id = "INVALID_404";

        when(patientQueryService.getPatientProfile(anyString())).thenThrow(new NotFoundException("Not found"));
        when(localRepository.findById(id)).thenReturn(Optional.empty());
        when(patientService.getPatientDetails(id)).thenThrow(new EntityNotFoundException("not found"));
        when(resilientRestTemplate.getForObjectWithFallback(anyString(), eq(PatientDetailsDTO.class)))
                .thenReturn(null);

        mockMvc.perform(get("/patients/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchWithoutNameParam_shouldReturn400() throws Exception {
        mockMvc.perform(get("/patients/search"))
                .andExpect(status().isBadRequest());
    }
}
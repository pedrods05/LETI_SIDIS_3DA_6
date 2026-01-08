package leti_sisdis_6.hapappointmentrecords.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import leti_sisdis_6.hapappointmentrecords.dto.input.AppointmentRecordRequest;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordResponse;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordViewDTO;
import leti_sisdis_6.hapappointmentrecords.dto.local.UserDTO;
import leti_sisdis_6.hapappointmentrecords.exceptions.NotFoundException;
import leti_sisdis_6.hapappointmentrecords.exceptions.UnauthorizedException;
import leti_sisdis_6.hapappointmentrecords.http.ExternalServiceClient;
import leti_sisdis_6.hapappointmentrecords.service.AppointmentRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AppointmentRecordControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AppointmentRecordService recordService;

    @Mock
    private ExternalServiceClient externalServiceClient;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AppointmentRecordController controller;

    private AppointmentRecordRequest validRequest;
    private AppointmentRecordViewDTO sampleRecordView;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        validRequest = new AppointmentRecordRequest();

        sampleRecordView = AppointmentRecordViewDTO.builder()
                .recordId("rec-123")
                .appointmentId("app-123")
                .diagnosis("Flu")
                .build();
    }

    @Test
    @DisplayName("POST /record - Sucesso (201)")
    void recordAppointmentDetails_Success() throws Exception {
        AppointmentRecordResponse response = AppointmentRecordResponse.builder().recordId("rec-123").build();

        when(recordService.createRecord(eq("app-123"), any(AppointmentRecordRequest.class), eq("phys-1")))
                .thenReturn(response);

        mockMvc.perform(post("/api/appointment-records/{appointmentId}/record", "app-123")
                        .header("X-User-Id", "phys-1") // Simula o header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordId").value("rec-123"));
    }

    @Test
    @DisplayName("POST /record - Erro 404 (Not Found Exception)")
    void recordAppointmentDetails_NotFound() throws Exception {
        when(recordService.createRecord(anyString(), any(), anyString()))
                .thenThrow(new NotFoundException("Appointment not found"));

        mockMvc.perform(post("/api/appointment-records/app-999/record")
                        .header("X-User-Id", "phys-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /{recordId} - Sucesso Local (200)")
    void viewAppointmentRecord_SuccessLocal() throws Exception {
        when(recordService.getAppointmentRecord(eq("rec-123"), any(UserDTO.class)))
                .thenReturn(sampleRecordView);

        mockMvc.perform(get("/api/appointment-records/{recordId}", "rec-123")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Flu"));
    }

    @Test
    @DisplayName("GET /{recordId} - Não encontrado Local, Encontrado no Peer (200)")
    void viewAppointmentRecord_PeerSuccess() throws Exception {
        when(recordService.getAppointmentRecord(anyString(), any()))
                .thenThrow(new NotFoundException("Not found locally"));

        when(externalServiceClient.getPeerUrls()).thenReturn(List.of("http://peer-one:8080"));

        ResponseEntity<AppointmentRecordViewDTO> remoteResponse = ResponseEntity.ok(sampleRecordView);

        when(restTemplate.exchange(
                eq("http://peer-one:8080/api/appointment-records/rec-123"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(AppointmentRecordViewDTO.class)
        )).thenReturn(remoteResponse);

        mockMvc.perform(get("/api/appointment-records/{recordId}", "rec-123")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId").value("rec-123"));
    }

    @Test
    @DisplayName("GET /{recordId} - Loop Infinito Prevenido (X-Peer-Request)")
    void viewAppointmentRecord_PreventLoop() throws Exception {
        // Se falhar localmente E tiver o header X-Peer-Request, deve parar imediatamente
        when(recordService.getAppointmentRecord(anyString(), any()))
                .thenThrow(new NotFoundException("Not found locally"));

        mockMvc.perform(get("/api/appointment-records/{recordId}", "rec-123")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Role", "ADMIN")
                        .header("X-Peer-Request", "true")) // Header crítico
                .andExpect(status().isNotFound());

        verify(externalServiceClient, never()).getPeerUrls();
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(AppointmentRecordViewDTO.class));
    }

    @Test
    @DisplayName("GET /{recordId} - Não encontrado em lugar nenhum (404)")
    void viewAppointmentRecord_NotFoundAnywhere() throws Exception {
        when(recordService.getAppointmentRecord(anyString(), any()))
                .thenThrow(new NotFoundException("Not found locally"));

        when(externalServiceClient.getPeerUrls()).thenReturn(List.of("http://peer-one:8080"));

        when(restTemplate.exchange(anyString(), any(), any(), eq(AppointmentRecordViewDTO.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/appointment-records/{recordId}", "rec-123")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /patient/{id} - Sucesso para Médico (200)")
    void getPatientRecords_SuccessPhysician() throws Exception {
        when(recordService.getPatientRecords("pat-1")).thenReturn(List.of(sampleRecordView));

        mockMvc.perform(get("/api/appointment-records/patient/{patientId}", "pat-1")
                        .header("X-User-Role", "PHYSICIAN")) // Role correta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recordId").value("rec-123"));
    }

    @Test
    @DisplayName("GET /patient/{id} - Proibido para Paciente (403)")
    void getPatientRecords_ForbiddenForPatient() throws Exception {

        mockMvc.perform(get("/api/appointment-records/patient/{patientId}", "pat-1")
                        .header("X-User-Role", "PATIENT")) // Role incorreta
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only physicians can view patient records"));

        verify(recordService, never()).getPatientRecords(anyString());
    }

    @Test
    @DisplayName("GET /patient/mine - Sucesso (200)")
    void getMyRecords_Success() throws Exception {
        when(recordService.getPatientRecords("pat-me")).thenReturn(List.of(sampleRecordView));

        mockMvc.perform(get("/api/appointment-records/patient/mine")
                        .header("X-User-Id", "pat-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].appointmentId").value("app-123"));
    }
}
package leti_sisdis_6.hapappointmentrecords.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import leti_sisdis_6.hapappointmentrecords.dto.input.AppointmentRecordRequest;
import leti_sisdis_6.hapappointmentrecords.dto.local.UserDTO;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordResponse;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordViewDTO;
import leti_sisdis_6.hapappointmentrecords.exceptions.NotFoundException;
import leti_sisdis_6.hapappointmentrecords.exceptions.UnauthorizedException;
import leti_sisdis_6.hapappointmentrecords.http.ExternalServiceClient;
import leti_sisdis_6.hapappointmentrecords.service.AppointmentRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AppointmentRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppointmentRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppointmentRecordService recordService;

    @MockBean
    private ExternalServiceClient externalServiceClient;

    @MockBean
    private RestTemplate restTemplate;

    private AppointmentRecordRequest validRequest() {
        AppointmentRecordRequest r = new AppointmentRecordRequest();
        r.setDiagnosis("Dx");
        r.setTreatmentRecommendations("TR");
        r.setPrescriptions("Rx");
        r.setDuration(LocalTime.of(0, 30));
        return r;
    }

    @Test
    @DisplayName("POST /api/appointment-records/{appointmentId}/record → 201 quando criado")
    void recordAppointmentDetails_created() throws Exception {
        var req = validRequest();
        var resp = AppointmentRecordResponse.builder()
                .message("Appointment record created successfully.")
                .appointmentId("A1")
                .recordId("REC1234")
                .build();
        given(recordService.createRecord(eq("A1"), any(AppointmentRecordRequest.class), eq("D1")))
                .willReturn(resp);

        mockMvc.perform(post("/api/appointment-records/A1/record")
                        .header("X-User-Id", "D1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordId", is("REC1234")));
    }

    @Test
    @DisplayName("POST /api/appointment-records/{appointmentId}/record → 404 NotFoundException")
    void recordAppointmentDetails_notFound() throws Exception {
        var req = validRequest();
        given(recordService.createRecord(eq("A2"), any(AppointmentRecordRequest.class), eq("D1")))
                .willThrow(new NotFoundException("Appointment not found"));

        mockMvc.perform(post("/api/appointment-records/A2/record")
                        .header("X-User-Id", "D1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Appointment not found")));
    }

    @Test
    @DisplayName("POST /api/appointment-records/{appointmentId}/record → 403 UnauthorizedException")
    void recordAppointmentDetails_forbidden() throws Exception {
        var req = validRequest();
        given(recordService.createRecord(eq("A3"), any(AppointmentRecordRequest.class), eq("D1")))
                .willThrow(new UnauthorizedException("not allowed"));

        mockMvc.perform(post("/api/appointment-records/A3/record")
                        .header("X-User-Id", "D1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("not allowed")));
    }

    @Test
    @DisplayName("POST /api/appointment-records/{appointmentId}/record → 409 IllegalStateException")
    void recordAppointmentDetails_conflict() throws Exception {
        var req = validRequest();
        given(recordService.createRecord(eq("A4"), any(AppointmentRecordRequest.class), eq("D1")))
                .willThrow(new IllegalStateException("Record already exists"));

        mockMvc.perform(post("/api/appointment-records/A4/record")
                        .header("X-User-Id", "D1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("Record already exists")));
    }

    @Test
    @DisplayName("POST /api/appointment-records/{appointmentId}/record → 500 em erro inesperado")
    void recordAppointmentDetails_internalError() throws Exception {
        var req = validRequest();
        given(recordService.createRecord(eq("A5"), any(AppointmentRecordRequest.class), eq("D1")))
                .willThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/appointment-records/A5/record")
                        .header("X-User-Id", "D1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", containsString("External service communication error")));
    }

    @Test
    @DisplayName("GET /api/appointment-records/{recordId} → 200 quando existe")
    void viewAppointmentRecord_ok() throws Exception {
        var dto = AppointmentRecordViewDTO.builder()
                .recordId("REC1")
                .appointmentId("A1")
                .physicianName("Dr. Who")
                .diagnosis("Dx")
                .treatmentRecommendations("TR")
                .prescriptions("Rx")
                .duration(LocalTime.of(0,30))
                .build();
        given(recordService.getAppointmentRecord(eq("REC1"), any(UserDTO.class)))
                .willReturn(dto);

        mockMvc.perform(get("/api/appointment-records/REC1")
                        .header("X-User-Id", "P1")
                        .header("X-User-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId", is("REC1")))
                .andExpect(jsonPath("$.physicianName", is("Dr. Who")));
    }

    @Test
    @DisplayName("GET /api/appointment-records/{recordId} → 200 quando encontrado num peer")
    void viewAppointmentRecord_foundInPeer() throws Exception {
        // Service throws NotFound -> controller tries peers
        given(recordService.getAppointmentRecord(eq("REC404"), any(UserDTO.class)))
                .willThrow(new NotFoundException("not here"));
        given(externalServiceClient.getPeerUrls()).willReturn(List.of("http://peer1"));
        var peerDto = AppointmentRecordViewDTO.builder()
                .recordId("REC404")
                .appointmentId("A1")
                .physicianName("Peer Doc")
                .diagnosis("Dx")
                .treatmentRecommendations("TR")
                .prescriptions("Rx")
                .duration(LocalTime.of(0,20))
                .build();
        given(restTemplate.exchange(
                eq("http://peer1/api/appointment-records/REC404"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(AppointmentRecordViewDTO.class)))
                .willReturn(ResponseEntity.ok(peerDto));

        mockMvc.perform(get("/api/appointment-records/REC404")
                        .header("X-User-Id", "P1")
                        .header("X-User-Role", "PATIENT")
                        .header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.physicianName", is("Peer Doc")));
    }

    @Test
    @DisplayName("GET /api/appointment-records/{recordId} → 404 quando não encontrado localmente nem em peers")
    void viewAppointmentRecord_notFoundAnywhere() throws Exception {
        given(recordService.getAppointmentRecord(eq("RECXXX"), any(UserDTO.class)))
                .willThrow(new NotFoundException("not here"));
        given(externalServiceClient.getPeerUrls()).willReturn(List.of("http://peer1", "http://peer2"));
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(AppointmentRecordViewDTO.class)))
                .willThrow(HttpClientErrorException.create(org.springframework.http.HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        mockMvc.perform(get("/api/appointment-records/RECXXX")
                        .header("X-User-Id", "P1")
                        .header("X-User-Role", "PATIENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/appointment-records/{recordId} → 403 UnauthorizedException")
    void viewAppointmentRecord_forbidden() throws Exception {
        given(recordService.getAppointmentRecord(eq("REC2"), any(UserDTO.class)))
                .willThrow(new UnauthorizedException("nope"));

        mockMvc.perform(get("/api/appointment-records/REC2")
                        .header("X-User-Id", "P1")
                        .header("X-User-Role", "PATIENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("nope")));
    }

    @Test
    @DisplayName("GET /api/appointment-records/{recordId} → 500 em erro inesperado")
    void viewAppointmentRecord_internalError() throws Exception {
        given(recordService.getAppointmentRecord(eq("REC3"), any(UserDTO.class)))
                .willThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/appointment-records/REC3")
                        .header("X-User-Id", "P1")
                        .header("X-User-Role", "PATIENT"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", containsString("External service communication error")));
    }

    @Test
    @DisplayName("GET /api/appointment-records/patient/mine → 200 lista")
    void getMyRecords_ok() throws Exception {
        given(recordService.getPatientRecords("P1")).willReturn(List.of());

        mockMvc.perform(get("/api/appointment-records/patient/mine")
                        .header("X-User-Id", "P1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/appointment-records/patient/mine → 403 UnauthorizedException")
    void getMyRecords_forbidden() throws Exception {
        given(recordService.getPatientRecords("P1")).willThrow(new UnauthorizedException("forbidden"));

        mockMvc.perform(get("/api/appointment-records/patient/mine")
                        .header("X-User-Id", "P1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("forbidden")));
    }

    @Test
    @DisplayName("GET /api/appointment-records/patient/mine → 500 erro inesperado")
    void getMyRecords_internalError() throws Exception {
        given(recordService.getPatientRecords("P1")).willThrow(new RuntimeException("err"));

        mockMvc.perform(get("/api/appointment-records/patient/mine")
                        .header("X-User-Id", "P1"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /api/appointment-records/patient/{id} → 200 lista")
    void getPatientRecords_ok() throws Exception {
        given(recordService.getPatientRecords("P2")).willReturn(List.of());

        mockMvc.perform(get("/api/appointment-records/patient/P2")
                        .header("X-User-Role", "PHYSICIAN"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/appointment-records/patient/{id} → 403 se role != PHYSICIAN")
    void getPatientRecords_wrongRole_forbidden() throws Exception {
        mockMvc.perform(get("/api/appointment-records/patient/P2")
                        .header("X-User-Role", "PATIENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/appointment-records/patient/{id} → 404 NotFoundException")
    void getPatientRecords_notFound() throws Exception {
        given(recordService.getPatientRecords("P2")).willThrow(new NotFoundException("patient not found"));

        mockMvc.perform(get("/api/appointment-records/patient/P2")
                        .header("X-User-Role", "PHYSICIAN"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/appointment-records/patient/{id} → 500 erro inesperado")
    void getPatientRecords_internalError() throws Exception {
        given(recordService.getPatientRecords("P2")).willThrow(new RuntimeException("err"));

        mockMvc.perform(get("/api/appointment-records/patient/P2")
                        .header("X-User-Role", "PHYSICIAN"))
                .andExpect(status().isInternalServerError());
    }
}

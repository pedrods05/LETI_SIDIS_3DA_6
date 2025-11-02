package leti_sisdis_6.hapappointmentrecords.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import leti_sisdis_6.hapappointmentrecords.http.ExternalServiceClient;
import leti_sisdis_6.hapappointmentrecords.model.Appointment;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentStatus;
import leti_sisdis_6.hapappointmentrecords.model.ConsultationType;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRepository;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AppointmentQueryController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AppointmentQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppointmentRepository appointmentRepository;

    // Mock to avoid JPA infrastructure (entityManagerFactory) in this WebMvc slice
    @MockitoBean
    private AppointmentRecordRepository appointmentRecordRepository;

    @MockitoBean
    private ExternalServiceClient externalServiceClient;

    @MockitoBean
    private RestTemplate restTemplate;

    private Appointment sampleAppointment(String id) {
        return Appointment.builder()
                .appointmentId(id)
                .patientId("p1")
                .physicianId("d1")
                .dateTime(LocalDateTime.of(2025, 1, 10, 10, 0))
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .build();
    }

    @Test
    @DisplayName("GET /api/appointments → deve listar todas")
    void listAll_returnsAll() throws Exception {
        given(appointmentRepository.findAll()).willReturn(List.of(sampleAppointment("a1"), sampleAppointment("a2")));

        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].appointmentId", is("a1")));
    }

    @Test
    @DisplayName("GET /api/appointments/{id} → devolve local quando existe")
    void getById_localFound() throws Exception {
        Appointment appt = sampleAppointment("a1");
        given(appointmentRepository.findById("a1")).willReturn(Optional.of(appt));

        mockMvc.perform(get("/api/appointments/a1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId", is("a1")));
    }

    @Test
    @DisplayName("GET /api/appointments/{id} → consulta peers quando não existe localmente e encontra")
    void getById_peerFound() throws Exception {
        given(appointmentRepository.findById("a2")).willReturn(Optional.empty());
        given(externalServiceClient.getPeerUrls()).willReturn(List.of("http://peer1"));
        given(restTemplate.getForObject("http://peer1/internal/appointments/a2", Appointment.class))
                .willReturn(sampleAppointment("a2"));

        mockMvc.perform(get("/api/appointments/a2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId", is("a2")));
    }

    @Test
    @DisplayName("GET /api/appointments/{id} → devolve 404 quando nem local nem peers")
    void getById_notFoundAnywhere() throws Exception {
        given(appointmentRepository.findById("a3")).willReturn(Optional.empty());
        given(externalServiceClient.getPeerUrls()).willReturn(List.of("http://peer1", "http://peer2"));
        given(restTemplate.getForObject(anyString(), eq(Appointment.class)))
                .willThrow(new RuntimeException("peer down"));

        mockMvc.perform(get("/api/appointments/a3"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/appointments/patient/{patientId} → devolve lista do repositório")
    void getByPatient() throws Exception {
        given(appointmentRepository.findByPatientId("p1")).willReturn(List.of(sampleAppointment("a1")));

        mockMvc.perform(get("/api/appointments/patient/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].patientId", is("p1")));
    }

    @Test
    @DisplayName("GET /api/appointments/physician/{physicianId} → devolve lista do repositório")
    void getByPhysician() throws Exception {
        given(appointmentRepository.findByPhysicianId("d1")).willReturn(List.of(sampleAppointment("a1")));

        mockMvc.perform(get("/api/appointments/physician/d1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].physicianId", is("d1")));
    }

    @Test
    @DisplayName("POST /api/appointments → 201 quando criado")
    void createAppointment_created() throws Exception {
        Appointment req = sampleAppointment("a10");
        given(appointmentRepository.existsById("a10")).willReturn(false);
        given(appointmentRepository.findByPhysicianIdAndDateTime(req.getPhysicianId(), req.getDateTime()))
                .willReturn(List.of());
        given(appointmentRepository.save(any(Appointment.class))).willAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointmentId", is("a10")));
    }

    @Test
    @DisplayName("POST /api/appointments → 409 quando id já existe")
    void createAppointment_conflictId() throws Exception {
        Appointment req = sampleAppointment("a11");
        given(appointmentRepository.existsById("a11")).willReturn(true);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/appointments → 409 quando conflito de horário do médico")
    void createAppointment_conflictTime() throws Exception {
        Appointment req = sampleAppointment("a12");
        given(appointmentRepository.existsById("a12")).willReturn(false);
        given(appointmentRepository.findByPhysicianIdAndDateTime(req.getPhysicianId(), req.getDateTime()))
                .willReturn(List.of(sampleAppointment("conflict")));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("Physician already has an appointment")));
    }

    @Test
    @DisplayName("POST /api/appointments → 400 em erro inesperado")
    void createAppointment_badRequestOnException() throws Exception {
        Appointment req = sampleAppointment("a13");
        given(appointmentRepository.existsById("a13")).willReturn(false);
        given(appointmentRepository.findByPhysicianIdAndDateTime(req.getPhysicianId(), req.getDateTime()))
                .willReturn(List.of());
        given(appointmentRepository.save(any(Appointment.class))).willThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("boom")));
    }

    @Test
    @DisplayName("PUT /api/appointments/{id} → 200 quando atualizado")
    void updateAppointment_ok() throws Exception {
        Appointment existing = sampleAppointment("a20");
        Appointment details = sampleAppointment("a20");
        details.setPatientId("p2");
        details.setPhysicianId("d2");
        details.setStatus(AppointmentStatus.CANCELLED);
        given(appointmentRepository.findById("a20")).willReturn(Optional.of(existing));
        given(appointmentRepository.save(any(Appointment.class))).willAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/appointments/a20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(details)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId", is("p2")))
                .andExpect(jsonPath("$.physicianId", is("d2")))
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("PUT /api/appointments/{id} → 404 quando não existe")
    void updateAppointment_notFound() throws Exception {
        Appointment details = sampleAppointment("a21");
        given(appointmentRepository.findById("a21")).willReturn(Optional.empty());

        mockMvc.perform(put("/api/appointments/a21")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(details)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/appointments/{id} → 400 em erro inesperado")
    void updateAppointment_badRequestOnException() throws Exception {
        Appointment existing = sampleAppointment("a22");
        Appointment details = sampleAppointment("a22");
        given(appointmentRepository.findById("a22")).willReturn(Optional.of(existing));
        given(appointmentRepository.save(any(Appointment.class))).willThrow(new RuntimeException("err"));

        mockMvc.perform(put("/api/appointments/a22")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(details)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("err")));
    }

    @Test
    @DisplayName("PUT /api/appointments/{id}/cancel → 200 e status CANCELLED")
    void cancelAppointment_ok() throws Exception {
        Appointment existing = sampleAppointment("a30");
        given(appointmentRepository.findById("a30")).willReturn(Optional.of(existing));
        given(appointmentRepository.save(any(Appointment.class))).willAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/appointments/a30/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("PUT /api/appointments/{id}/cancel → 404 quando não existe")
    void cancelAppointment_notFound() throws Exception {
        given(appointmentRepository.findById("a31")).willReturn(Optional.empty());

        mockMvc.perform(put("/api/appointments/a31/cancel"))
                .andExpect(status().isNotFound());
    }

}

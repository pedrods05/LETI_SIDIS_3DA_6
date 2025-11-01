package leti_sisdis_6.hapappointmentrecords.api;

import leti_sisdis_6.hapappointmentrecords.model.Appointment;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentStatus;
import leti_sisdis_6.hapappointmentrecords.model.ConsultationType;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentRepository appointmentRepository;

    private Appointment sample(String id) {
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
    @DisplayName("GET /internal/appointments/{id} → 200 quando existe")
    void getInternal_found() throws Exception {
        given(appointmentRepository.findById("A1")).willReturn(Optional.of(sample("A1")));

        mockMvc.perform(get("/internal/appointments/A1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId", is("A1")));
    }

    @Test
    @DisplayName("GET /internal/appointments/{id} → 404 quando não existe")
    void getInternal_notFound() throws Exception {
        given(appointmentRepository.findById("A2")).willReturn(Optional.empty());

        mockMvc.perform(get("/internal/appointments/A2"))
                .andExpect(status().isNotFound());
    }
}

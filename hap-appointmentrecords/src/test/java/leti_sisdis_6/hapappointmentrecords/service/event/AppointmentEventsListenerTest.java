package leti_sisdis_6.hapappointmentrecords.service.event;

import leti_sisdis_6.hapappointmentrecords.model.AppointmentStatus;
import leti_sisdis_6.hapappointmentrecords.model.ConsultationType;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentProjectionRepository;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

class AppointmentEventsListenerTest {

    private AppointmentProjectionRepository projectionRepository;
    private AppointmentRepository appointmentRepository;
    private AppointmentEventsListener listener;

    @BeforeEach
    void setUp() {
        projectionRepository = mock(AppointmentProjectionRepository.class);
        appointmentRepository = mock(AppointmentRepository.class);
        listener = new AppointmentEventsListener(projectionRepository, appointmentRepository);
    }

    @Test
    void onAppointmentCreated_savesProjectionAndAppointment() {
        var event = new AppointmentCreatedEvent("a1", "p1", "d1", LocalDateTime.of(2025,12,10,9,0), ConsultationType.FIRST_TIME, AppointmentStatus.SCHEDULED, LocalDateTime.now());

        listener.onAppointmentCreated(event);

        verify(projectionRepository, times(1)).save(any());
        verify(appointmentRepository, times(1)).save(any());
    }
}

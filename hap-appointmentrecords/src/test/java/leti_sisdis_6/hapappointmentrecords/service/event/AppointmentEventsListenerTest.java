package leti_sisdis_6.hapappointmentrecords.service.event;

import leti_sisdis_6.hapappointmentrecords.model.AppointmentStatus;
import leti_sisdis_6.hapappointmentrecords.model.ConsultationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

/**
 * Tests for AppointmentEventsListener
 * Note: Listener now only logs events - no data storage
 */
@ExtendWith(MockitoExtension.class)
class AppointmentEventsListenerTest {

    @InjectMocks
    private AppointmentEventsListener listener;

    @Test
    @DisplayName("Deve processar evento de criação de appointment sem erros")
    void shouldProcessAppointmentCreatedEventWithoutError() {
        // Given
        AppointmentCreatedEvent event = new AppointmentCreatedEvent(
                "APT001",
                "PAT001",
                "PHY001",
                LocalDateTime.of(2025, 11, 1, 10, 0),
                ConsultationType.FIRST_TIME,
                AppointmentStatus.SCHEDULED,
                LocalDateTime.now()
        );

        // When / Then - should not throw exception
        listener.onAppointmentCreated(event);
    }

    @Test
    @DisplayName("Deve processar evento de atualização de appointment sem erros")
    void shouldProcessAppointmentUpdatedEventWithoutError() {
        // Given
        AppointmentUpdatedEvent event = new AppointmentUpdatedEvent(
                "APT001",
                "PAT001",
                "PHY001",
                LocalDateTime.of(2025, 11, 1, 10, 0),
                ConsultationType.FIRST_TIME,
                AppointmentStatus.SCHEDULED,
                AppointmentStatus.COMPLETED,
                LocalDateTime.now()
        );

        // When / Then - should not throw exception
        listener.onAppointmentUpdated(event, null, null);
    }

    @Test
    @DisplayName("Deve processar evento de cancelamento de appointment sem erros")
    void shouldProcessAppointmentCanceledEventWithoutError() {
        // Given
        AppointmentCanceledEvent event = new AppointmentCanceledEvent(
                "APT001",
                "PAT001",
                "PHY001",
                LocalDateTime.of(2025, 11, 1, 10, 0),
                ConsultationType.FIRST_TIME,
                AppointmentStatus.SCHEDULED,
                AppointmentStatus.CANCELLED,
                "Patient request",
                LocalDateTime.now()
        );

        // When / Then - should not throw exception
        listener.onAppointmentCanceled(event, null, null);
    }
}

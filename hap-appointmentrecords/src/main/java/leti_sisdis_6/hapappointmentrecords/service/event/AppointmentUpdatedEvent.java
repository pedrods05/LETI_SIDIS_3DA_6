package leti_sisdis_6.hapappointmentrecords.service.event;

import java.time.LocalDateTime;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentStatus;
import leti_sisdis_6.hapappointmentrecords.model.ConsultationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentUpdatedEvent {

    private String appointmentId;
    private String patientId;
    private String physicianId;
    private LocalDateTime dateTime;
    private ConsultationType consultationType;
    private AppointmentStatus previousStatus;
    private AppointmentStatus newStatus;
    private LocalDateTime occurredAt;
}

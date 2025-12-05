package leti_sisdis_6.hapappointmentrecords.service.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRecordCreatedEvent {
    private String recordId;
    private String appointmentId;
    private String patientId;
    private String physicianId;
    private LocalDateTime occurredAt;
}


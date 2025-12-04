package leti_sisdis_6.happhysicians.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentUpdatedEvent implements Serializable {
    private String appointmentId;
    private String patientId;
    private String physicianId;
    private LocalDateTime dateTime;
    private String consultationType;
    private String status;
}


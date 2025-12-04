package leti_sisdis_6.happhysicians.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentReminderEvent implements Serializable {
    private String appointmentId;
    private String patientId;
    private String patientName;
    private String patientEmail;
    private String patientPhone;
    private String physicianId;
    private String physicianName;
    private LocalDateTime dateTime;
    private String consultationType;
    private String reminderType; // "CREATED", "UPDATED", "UPCOMING"
}


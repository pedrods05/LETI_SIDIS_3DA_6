package leti_sisdis_6.happhysicians.dto.output;

import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDetailsDTO {
    private String appointmentId;
    private String patientId;
    private String patientName;
    private String physicianId;
    private String physicianName;
    private LocalDateTime dateTime;
    private ConsultationType consultationType;
    private AppointmentStatus status;
}


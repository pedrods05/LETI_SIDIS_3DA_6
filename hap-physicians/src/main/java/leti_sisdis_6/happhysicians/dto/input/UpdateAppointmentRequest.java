package leti_sisdis_6.happhysicians.dto.input;

import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class UpdateAppointmentRequest {
    private String patientId;
    private String physicianId;
    private LocalDateTime dateTime;
    private ConsultationType consultationType;
    private AppointmentStatus status;
    private Boolean wasRescheduled;
}

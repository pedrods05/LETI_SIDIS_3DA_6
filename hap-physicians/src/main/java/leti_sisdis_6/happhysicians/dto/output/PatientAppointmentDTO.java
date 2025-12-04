package leti_sisdis_6.happhysicians.dto.output;


import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientAppointmentDTO {
    private String physicianName;
    private LocalDateTime dateTime;
    private ConsultationType consultationType;
    private AppointmentStatus status;
}
package leti_sisdis_6.happhysicians.dto.output;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentListDTO {
    private LocalDate date;
    private LocalTime time;
    private String patientName;
    private String physicianName;
}

package leti_sisdis_6.happhysicians.dto.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AppointmentIdResponse {
    private String appointmentId;
    private String status;
    private String message;
}

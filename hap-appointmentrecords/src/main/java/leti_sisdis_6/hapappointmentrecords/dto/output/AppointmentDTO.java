package leti_sisdis_6.hapappointmentrecords.dto.output;

import lombok.Data;

@Data
public class AppointmentDTO {
    private String appointmentId;
    private String physicianId;
    private String physicianName;
    private String patientId;
    private String dateTime;
}

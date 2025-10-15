package leti_sisdis_6.hapappointmentrecords.dto.input;

import leti_sisdis_6.hapappointmentrecords.model.ConsultationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class ScheduleAppointmentRequest {

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    @NotBlank(message = "Physician ID is required")
    private String physicianId;

    @NotNull(message = "Date and time is required")
    private LocalDateTime dateTime;

    @NotNull(message = "Consultation type is required")
    private ConsultationType consultationType;
} 
package leti_sisdis_6.happhysicians.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class ScheduleAppointmentRequest {

    @NotBlank(message = "Appointment ID is required")
    private String appointmentId;

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    @NotBlank(message = "Physician ID is required")
    private String physicianId;

    @NotNull(message = "Date and time is required")
    private LocalDateTime dateTime;

    @NotNull(message = "Consultation type is required")
    private ConsultationType consultationType;

    @NotNull(message = "Status is required")
    private AppointmentStatus status;

    private Boolean wasRescheduled;
}

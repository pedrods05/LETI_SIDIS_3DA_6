package leti_sisdis_6.happhysicians.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentRequestDTO {
    @NotBlank(message = "Physician ID is required")
    private String physicianId;

    @NotNull(message = "Date and time is required")
    private LocalDateTime dateTime;

    @NotBlank(message = "Consultation type is required")
    private String consultationType;
}

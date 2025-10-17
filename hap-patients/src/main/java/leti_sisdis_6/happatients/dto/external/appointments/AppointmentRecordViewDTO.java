package leti_sisdis_6.happatients.dto.external.appointments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRecordViewDTO {
    private String recordId;
    private String appointmentId;
    private String physicianName;
    private String diagnosis;
    private String treatmentRecommendations;
    private String prescriptions;
    private LocalTime duration;
}


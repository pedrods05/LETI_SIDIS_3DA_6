package leti_sisdis_6.happhysicians.dto.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRecordDTO {
    private String recordId;
    private String appointmentId;
    private String physicianId;
    private String patientId;
    private String diagnosis;
    private String treatment;
    private List<String> prescriptions;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

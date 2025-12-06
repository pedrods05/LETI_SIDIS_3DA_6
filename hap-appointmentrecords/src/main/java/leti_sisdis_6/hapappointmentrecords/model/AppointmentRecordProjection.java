package leti_sisdis_6.hapappointmentrecords.model;

import java.time.LocalTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "appointment_record_projections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRecordProjection {
    @Id
    private String recordId;

    private String appointmentId;
    private String patientId;
    private String physicianId;

    // Clinical details for read
    private String diagnosis;
    private String treatmentRecommendations;
    private String prescriptions;
    private LocalTime duration;
}

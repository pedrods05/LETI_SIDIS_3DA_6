package leti_sisdis_6.happhysicians.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "appointment_summaries")
public class AppointmentSummary {
    @Id
    private String id;
    private String patientId;
    private String physicianId;
    private LocalDateTime dateTime;
    private String consultationType;
    private String status;
}


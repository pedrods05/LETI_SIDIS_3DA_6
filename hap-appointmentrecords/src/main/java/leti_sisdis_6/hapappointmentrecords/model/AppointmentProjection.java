package leti_sisdis_6.hapappointmentrecords.model;

import java.time.LocalDateTime;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentStatus;
import leti_sisdis_6.hapappointmentrecords.model.ConsultationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "appointment_projections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentProjection {

    @Id
    private String appointmentId;

    private String patientId;

    private String physicianId;

    private LocalDateTime dateTime;

    private ConsultationType consultationType;

    private AppointmentStatus status;

    private LocalDateTime lastUpdated;
}

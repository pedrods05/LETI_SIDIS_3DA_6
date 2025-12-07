package leti_sisdis_6.hapappointmentrecords.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "appointment_records")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AppointmentRecord {

    @Id
    @Column(name = "record_id", length = 32, nullable = false, updatable = false)
    private String recordId;

    // Store only appointmentId - full appointment data lives in physicians service
    @Column(name = "appointment_id", length = 255, nullable = false, unique = true)
    private String appointmentId;

    @Column(nullable = false, length = 255)
    private String diagnosis;

    @Column(nullable = false, length = 500)
    private String treatmentRecommendations;

    @Column(nullable = false, length = 500)
    private String prescriptions;

    @Column(nullable = false)
    private LocalTime duration;
}

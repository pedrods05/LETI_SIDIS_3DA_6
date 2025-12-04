package leti_sisdis_6.hapappointmentrecords.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments_projection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentProjection {

    @Id
    @Column(name = "appointment_id", length = 32, nullable = false, updatable = false)
    private String appointmentId;

    @Column(name = "patient_id", length = 32, nullable = false)
    private String patientId;

    @Column(name = "physician_id", length = 32, nullable = false)
    private String physicianId;

    @Column(nullable = false)
    private LocalDateTime dateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsultationType consultationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}

package leti_sisdis_6.happhysicians.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @Column(nullable = false, length = 10)
    private String appointmentId;

    @Column(nullable = false)
    private String patientId;

    @Column
    private String patientName;

    @Column
    private String patientEmail;

    @Column
    private String patientPhone;

    @ManyToOne(optional = false)
    @JoinColumn(name = "physician_id")
    private Physician physician;

    @Column(nullable = false)
    private LocalDateTime dateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsultationType consultationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Column(nullable = false)
    private boolean wasRescheduled;
}

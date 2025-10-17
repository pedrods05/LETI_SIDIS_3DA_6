package leti_sisdis_6.hapappointmentrecords.model;

import jakarta.persistence.*;
import leti_sisdis_6.happhysicians.model.Appointment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "appointment_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRecord {
    @Id
    @Column(length = 10)
    private String recordId;

    @OneToOne
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String diagnosis;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String treatmentRecommendations;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prescriptions;

    @Column(nullable = false)
    private LocalTime duration;
} 
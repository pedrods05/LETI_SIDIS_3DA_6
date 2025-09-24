package com.pcm.psoft.pcmclinic_api.appointment.model;

import com.pcm.psoft.pcmclinic_api.patient.model.Patient;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.Physician;
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
    @Column(length = 10)
    private String appointmentId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

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

    @OneToOne(mappedBy = "appointment", cascade = CascadeType.ALL)
    private AppointmentRecord record;
}

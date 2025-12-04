package leti_sisdis_6.happatients.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "patient_events")
public class PatientEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String eventType;

    @Column(nullable = false, length = 20)
    private String patientId;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    protected PatientEvent() {
        // for JPA
    }

    public PatientEvent(String eventType, String patientId, String description, LocalDateTime occurredAt) {
        this.eventType = eventType;
        this.patientId = patientId;
        this.description = description;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}

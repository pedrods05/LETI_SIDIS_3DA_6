package leti_sisdis_6.hapappointmentrecords.service.event;

import java.time.LocalDateTime;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentStatus;
import leti_sisdis_6.hapappointmentrecords.model.ConsultationType;

public class AppointmentCreatedEvent {
    private String appointmentId;
    private String patientId;
    private String physicianId;
    private LocalDateTime dateTime;
    private ConsultationType consultationType;
    private AppointmentStatus status;
    private LocalDateTime occurredAt;

    public AppointmentCreatedEvent() {
    }

    public AppointmentCreatedEvent(String appointmentId, String patientId, String physicianId, LocalDateTime dateTime, ConsultationType consultationType, AppointmentStatus status, LocalDateTime occurredAt) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.physicianId = physicianId;
        this.dateTime = dateTime;
        this.consultationType = consultationType;
        this.status = status;
        this.occurredAt = occurredAt;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPhysicianId() {
        return physicianId;
    }

    public void setPhysicianId(String physicianId) {
        this.physicianId = physicianId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public ConsultationType getConsultationType() {
        return consultationType;
    }

    public void setConsultationType(ConsultationType consultationType) {
        this.consultationType = consultationType;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}

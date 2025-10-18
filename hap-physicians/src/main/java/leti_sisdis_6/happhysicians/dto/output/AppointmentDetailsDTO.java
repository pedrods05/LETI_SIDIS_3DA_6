package leti_sisdis_6.happhysicians.dto.output;

import leti_sisdis_6.happhysicians.dto.external.AppointmentRecordDTO;
import leti_sisdis_6.happhysicians.dto.external.PatientDTO;
import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDetailsDTO {
    private String appointmentId;
    private String patientId;
    private String patientName;
    private String physicianId;
    private String physicianName;

    private LocalDateTime dateTime;
    private ConsultationType consultationType;
    private AppointmentStatus status;
    private boolean wasRescheduled;
    
    // Enriched data from other microservices
    private PatientDTO patient;
    private AppointmentRecordDTO appointmentRecord;
    
    public AppointmentDetailsDTO(Appointment appointment, PatientDTO patient) {
        this.appointmentId = appointment.getAppointmentId();
        this.patientId = appointment.getPatientId();
        this.physicianId = appointment.getPhysician().getPhysicianId();
        this.dateTime = appointment.getDateTime();
        this.consultationType = appointment.getConsultationType();
        this.status = appointment.getStatus();
        this.wasRescheduled = appointment.isWasRescheduled();
        this.patient = patient;
    }
    
    public AppointmentDetailsDTO(Appointment appointment, PatientDTO patient, AppointmentRecordDTO appointmentRecord) {
        this(appointment, patient);
        this.appointmentRecord = appointmentRecord;
    }
}
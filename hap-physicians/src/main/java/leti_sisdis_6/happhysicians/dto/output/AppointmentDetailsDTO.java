package leti_sisdis_6.happhysicians.dto.output;

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

    // Basic patient info (from local data only)
    private String patientEmail;
    private String patientPhone;

    public AppointmentDetailsDTO(Appointment appointment) {
        this.appointmentId = appointment.getAppointmentId();
        this.patientId = appointment.getPatientId();
        this.patientName = appointment.getPatientName();
        this.patientEmail = appointment.getPatientEmail();
        this.patientPhone = appointment.getPatientPhone();
        this.physicianId = appointment.getPhysician().getPhysicianId();
        this.physicianName = appointment.getPhysician().getFullName();
        this.dateTime = appointment.getDateTime();
        this.consultationType = appointment.getConsultationType();
        this.status = appointment.getStatus();
        this.wasRescheduled = appointment.isWasRescheduled();
    }
}

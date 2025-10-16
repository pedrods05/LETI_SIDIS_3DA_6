package leti_sisdis_6.happhysicians.services;

import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PhysicianRepository physicianRepository;

    public Appointment createAppointment(Appointment appointment) {
        // Validate physician exists
        Optional<Physician> physician = physicianRepository.findById(appointment.getPhysician().getPhysicianId());
        if (physician.isEmpty()) {
            throw new RuntimeException("Physician not found");
        }

        // Check for conflicts
        if (appointmentRepository.existsByPhysicianPhysicianIdAndDateTime(
                appointment.getPhysician().getPhysicianId(), 
                appointment.getDateTime())) {
            throw new RuntimeException("Physician already has an appointment at this time");
        }

        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getAppointmentsByPhysician(String physicianId) {
        return appointmentRepository.findByPhysicianPhysicianId(physicianId);
    }

    public List<Appointment> getAppointmentsByPatient(String patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public Optional<Appointment> getAppointmentById(String appointmentId) {
        return appointmentRepository.findById(appointmentId);
    }

    public List<Appointment> getAppointmentsByDateRange(LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findByDateTimeBetween(start, end);
    }

    public Appointment updateAppointment(String appointmentId, Appointment appointmentDetails) {
        Optional<Appointment> optionalAppointment = appointmentRepository.findById(appointmentId);
        if (optionalAppointment.isPresent()) {
            Appointment appointment = optionalAppointment.get();
            // appointmentId não pode ser alterado pois é a chave primária
            appointment.setPatientId(appointmentDetails.getPatientId());
            appointment.setPhysician(appointmentDetails.getPhysician());
            appointment.setDateTime(appointmentDetails.getDateTime());
            appointment.setConsultationType(appointmentDetails.getConsultationType());
            appointment.setStatus(appointmentDetails.getStatus());
            appointment.setWasRescheduled(appointmentDetails.isWasRescheduled());

            return appointmentRepository.save(appointment);
        }
        return null;
    }

    public boolean deleteAppointment(String appointmentId) {
        if (appointmentRepository.existsById(appointmentId)) {
            appointmentRepository.deleteById(appointmentId);
            return true;
        }
        return false;
    }
}

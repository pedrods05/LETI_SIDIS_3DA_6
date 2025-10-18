package leti_sisdis_6.happhysicians.services;

import leti_sisdis_6.happhysicians.dto.external.AppointmentRecordDTO;
import leti_sisdis_6.happhysicians.dto.external.PatientDTO;
import leti_sisdis_6.happhysicians.dto.output.AppointmentDetailsDTO;
import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import leti_sisdis_6.happhysicians.services.ExternalServiceClient;
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

    @Autowired
    private ExternalServiceClient externalServiceClient;

    public Appointment createAppointment(Appointment appointment) {
        // Validate physician exists and set the complete physician object
        Optional<Physician> physician = physicianRepository.findById(appointment.getPhysician().getPhysicianId());
        if (physician.isEmpty()) {
            throw new RuntimeException("Physician not found");
        }
        
        // Set the complete physician object
        appointment.setPhysician(physician.get());

        // Check for conflicts
        if (appointmentRepository.existsByPhysicianPhysicianIdAndDateTime(
                appointment.getPhysician().getPhysicianId(), 
                appointment.getDateTime())) {
            throw new RuntimeException("Physician already has an appointment at this time");
        }

        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
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

    // ===== MÉTODOS DE COMUNICAÇÃO INTER-MICROSERVIÇOS =====

    public AppointmentDetailsDTO getAppointmentWithPatient(String appointmentId) {
        // Get consultation details locally
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        // Call Patient service for patient details
        PatientDTO patient = externalServiceClient.getPatientById(appointment.getPatientId());

        // Combine data and return
        return new AppointmentDetailsDTO(appointment, patient);
    }

    public AppointmentDetailsDTO getAppointmentWithPatientAndRecord(String appointmentId) {
        // Get consultation details locally
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        // Call Patient service for patient details
        PatientDTO patient = externalServiceClient.getPatientById(appointment.getPatientId());

        // Call Appointment Records service for record details
        try {
            AppointmentRecordDTO appointmentRecord = externalServiceClient.getAppointmentRecord(appointmentId);
            return new AppointmentDetailsDTO(appointment, patient, appointmentRecord);
        } catch (Exception e) {
            // If no record exists yet, return without it
            return new AppointmentDetailsDTO(appointment, patient);
        }
    }

    public List<AppointmentDetailsDTO> getAppointmentsByPhysicianWithPatients(String physicianId) {
        List<Appointment> appointments = appointmentRepository.findByPhysicianPhysicianId(physicianId);

        return appointments.stream()
                .map(appointment -> {
                    try {
                        PatientDTO patient = externalServiceClient.getPatientById(appointment.getPatientId());
                        return new AppointmentDetailsDTO(appointment, patient);
                    } catch (Exception e) {
                        // Return appointment without patient data if service is unavailable
                        return new AppointmentDetailsDTO(appointment, null);
                    }
                })
                .toList();
    }
}

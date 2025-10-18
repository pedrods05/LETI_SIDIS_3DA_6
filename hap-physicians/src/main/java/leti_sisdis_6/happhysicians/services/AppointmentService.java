package leti_sisdis_6.happhysicians.services;

import leti_sisdis_6.happhysicians.dto.output.AppointmentDetailsDTO;
import leti_sisdis_6.happhysicians.exceptions.AppointmentRecordNotFoundException;
import leti_sisdis_6.happhysicians.exceptions.PatientNotFoundException;
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
import java.util.Map;
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
        try {
            Map<String, Object> patientData = externalServiceClient.getPatientById(appointment.getPatientId());
            // Update appointment with patient data from external service
            appointment.setPatientName((String) patientData.get("fullName"));
            appointment.setPatientEmail((String) patientData.get("email"));
            appointment.setPatientPhone((String) patientData.get("phoneNumber"));
            return new AppointmentDetailsDTO(appointment);
        } catch (PatientNotFoundException e) {
            // Return appointment with local data only
            return new AppointmentDetailsDTO(appointment);
        }
    }

    public AppointmentDetailsDTO getAppointmentWithPatientAndRecord(String appointmentId) {
        // Get consultation details locally
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        // Call Patient service for patient details
        try {
            Map<String, Object> patientData = externalServiceClient.getPatientById(appointment.getPatientId());
            appointment.setPatientName((String) patientData.get("fullName"));
            appointment.setPatientEmail((String) patientData.get("email"));
            appointment.setPatientPhone((String) patientData.get("phoneNumber"));
        } catch (PatientNotFoundException e) {
            // Continue without patient data
        }

        // Call Appointment Records service for record details
        try {
            Map<String, Object> appointmentRecord = externalServiceClient.getAppointmentRecord(appointmentId);
            // Could store record data in appointment if needed
        } catch (AppointmentRecordNotFoundException e) {
            // Continue without record data
        }

        return new AppointmentDetailsDTO(appointment);
    }

    public List<AppointmentDetailsDTO> getAppointmentsByPhysicianWithPatients(String physicianId) {
        List<Appointment> appointments = appointmentRepository.findByPhysicianPhysicianId(physicianId);

        return appointments.stream()
                .map(appointment -> {
                    try {
                        Map<String, Object> patientData = externalServiceClient.getPatientById(appointment.getPatientId());
                        appointment.setPatientName((String) patientData.get("fullName"));
                        appointment.setPatientEmail((String) patientData.get("email"));
                        appointment.setPatientPhone((String) patientData.get("phoneNumber"));
                        return new AppointmentDetailsDTO(appointment);
                    } catch (PatientNotFoundException e) {
                        // Return appointment with local data only
                        return new AppointmentDetailsDTO(appointment);
                    }
                })
                .toList();
    }
}

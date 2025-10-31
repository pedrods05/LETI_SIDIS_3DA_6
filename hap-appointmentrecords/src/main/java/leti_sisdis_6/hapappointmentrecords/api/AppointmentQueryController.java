package leti_sisdis_6.hapappointmentrecords.api;

import leti_sisdis_6.hapappointmentrecords.model.Appointment;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentQueryController {

    private final AppointmentRepository appointmentRepository;

    @GetMapping
    public List<Appointment> listAll() {
        return appointmentRepository.findAll();
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<Appointment> getById(@PathVariable String appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/patient/{patientId}")
    public List<Appointment> getByPatient(@PathVariable String patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    @GetMapping("/physician/{physicianId}")
    public List<Appointment> getByPhysician(@PathVariable String physicianId) {
        return appointmentRepository.findByPhysicianId(physicianId);
    }

    @PostMapping
    public ResponseEntity<?> createAppointment(@RequestBody Appointment appointment) {
        try {
            // Check if appointment already exists
            if (appointmentRepository.existsById(appointment.getAppointmentId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Appointment with ID " + appointment.getAppointmentId() + " already exists"));
            }

            // Check for time conflicts
            List<Appointment> conflictingAppointments = appointmentRepository
                    .findByPhysicianIdAndDateTime(appointment.getPhysicianId(), appointment.getDateTime());

            if (!conflictingAppointments.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Physician already has an appointment at this time"));
            }

            Appointment saved = appointmentRepository.save(appointment);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{appointmentId}")
    public ResponseEntity<?> updateAppointment(
            @PathVariable String appointmentId,
            @RequestBody Appointment appointmentDetails) {
        try {
            return appointmentRepository.findById(appointmentId)
                    .map(existing -> {
                        existing.setPatientId(appointmentDetails.getPatientId());
                        existing.setPhysicianId(appointmentDetails.getPhysicianId());
                        existing.setDateTime(appointmentDetails.getDateTime());
                        existing.setConsultationType(appointmentDetails.getConsultationType());
                        existing.setStatus(appointmentDetails.getStatus());
                        Appointment updated = appointmentRepository.save(existing);
                        return ResponseEntity.ok(updated);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<?> deleteAppointment(@PathVariable String appointmentId) {
        if (appointmentRepository.existsById(appointmentId)) {
            appointmentRepository.deleteById(appointmentId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

package leti_sisdis_6.happhysicians.api;

import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import leti_sisdis_6.happhysicians.services.ExternalServiceClient;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Internal controller for peer-to-peer communication.
 * These endpoints are not exposed in Swagger and are used for internal communication between instances.
 */
@RestController
@RequestMapping("/internal")
@Hidden
public class InternalController {

    @Autowired
    private PhysicianRepository physicianRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ExternalServiceClient externalServiceClient;

    @Autowired
    private RestTemplate restTemplate;

    // ===== INTERNAL PHYSICIAN ENDPOINTS =====

    /**
     * Internal endpoint to get physician by ID (for peer communication)
     */
    @GetMapping("/physicians/{physicianId}")
    public ResponseEntity<Physician> getPhysicianInternal(@PathVariable String physicianId) {
        Optional<Physician> physician = physicianRepository.findById(physicianId);
        return physician.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Internal endpoint to get all physicians (for peer communication)
     */
    @GetMapping("/physicians")
    public List<Physician> getAllPhysiciansInternal() {
        return physicianRepository.findAll();
    }

    // ===== INTERNAL APPOINTMENT ENDPOINTS =====

    /**
     * Internal endpoint to get appointment by ID (for peer communication)
     */
    @GetMapping("/appointments/{appointmentId}")
    public ResponseEntity<Appointment> getAppointmentInternal(@PathVariable String appointmentId) {
        Optional<Appointment> appointment = appointmentRepository.findById(appointmentId);
        return appointment.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Internal endpoint to get all appointments (for peer communication)
     */
    @GetMapping("/appointments")
    public List<Appointment> getAllAppointmentsInternal() {
        return appointmentRepository.findAll();
    }

    // ===== PEER MANAGEMENT ENDPOINTS =====

    /**
     * Get list of peer instances (internal)
     */
    @GetMapping("/peers")
    public List<String> getPeers() {
        return externalServiceClient.getPeerUrls();
    }

    /**
     * Get peer status information (internal)
     */
    @GetMapping("/peers/status")
    public Map<String, Object> getPeerStatus() {
        return Map.of(
            "currentInstance", externalServiceClient.getCurrentInstanceUrl(),
            "totalPeers", externalServiceClient.getPeerCount(),
            "hasPeers", externalServiceClient.hasPeers(),
            "peerUrls", externalServiceClient.getPeerUrls()
        );
    }
}

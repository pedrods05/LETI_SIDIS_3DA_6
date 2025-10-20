package leti_sisdis_6.happhysicians.services;

import leti_sisdis_6.happhysicians.exceptions.AppointmentRecordNotFoundException;
import leti_sisdis_6.happhysicians.exceptions.MicroserviceCommunicationException;
import leti_sisdis_6.happhysicians.exceptions.PatientNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExternalServiceClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${hap.patients.base-url:http://localhost:8082}")
    private String patientsServiceUrl;

    @Value("${hap.auth.base-url:http://localhost:8080}")
    private String authServiceUrl;

    @Value("${hap.appointmentrecords.base-url:http://localhost:8083}")
    private String appointmentRecordsServiceUrl;

    @Value("${server.port:8081}")
    private String currentPort;

    // Hardcoded peer lists (Initial Approach) - as per slide
    private final List<String> peers = Arrays.asList(
        "http://localhost:8081",
        "http://localhost:8087"
    );

    // Patient Service calls
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public Map<String, Object> getPatientById(String patientId) {
        String url = patientsServiceUrl + "/patients/" + patientId;
        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new PatientNotFoundException("Patient not found with id: " + patientId, e);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new MicroserviceCommunicationException("Patients", "getPatientById", "Unauthorized to access patient data", e);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new MicroserviceCommunicationException("Patients", "getPatientById", "Forbidden to access patient data", e);
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("Patients", "getPatientById", e.getMessage(), e);
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPatientsByIds(List<String> patientIds) {
        String url = patientsServiceUrl + "/patients/batch";
        try {
            return restTemplate.postForObject(url, patientIds, List.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new MicroserviceCommunicationException("Patients", "getPatientsByIds", "Unauthorized to access patient data", e);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new MicroserviceCommunicationException("Patients", "getPatientsByIds", "Forbidden to access patient data", e);
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("Patients", "getPatientsByIds", e.getMessage(), e);
        }
    }

    // Auth Service calls
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public Map<String, Object> getUserById(String userId) {
        String url = authServiceUrl + "/users/" + userId;
        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new MicroserviceCommunicationException("Auth", "getUserById", "User not found with id: " + userId, e);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new MicroserviceCommunicationException("Auth", "getUserById", "Unauthorized to access user data", e);
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("Auth", "getUserById", e.getMessage(), e);
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public Map<String, Object> validateToken(String token) {
        String url = authServiceUrl + "/auth/validate";
        try {
            return restTemplate.postForObject(url, token, Map.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new MicroserviceCommunicationException("Auth", "validateToken", "Invalid or expired token", e);
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("Auth", "validateToken", e.getMessage(), e);
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public Map<String, Object> registerUser(String username, String password, String role) {
        String url = authServiceUrl + "/api/public/register";

        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        request.put("password", password);
        request.put("role", role);

        try {
            return restTemplate.postForObject(url, request, Map.class);
        } catch (HttpClientErrorException.Conflict e) {
            throw new MicroserviceCommunicationException("Auth", "registerUser", "Username already exists", e);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new MicroserviceCommunicationException("Auth", "registerUser", "Invalid request data", e);
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("Auth", "registerUser", e.getMessage(), e);
        }
    }

    // Appointment Records Service calls
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public Map<String, Object> getAppointmentRecord(String appointmentId) {
        String url = appointmentRecordsServiceUrl + "/appointment-records/appointment/" + appointmentId;
        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new AppointmentRecordNotFoundException("Appointment record not found for appointment: " + appointmentId, e);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new MicroserviceCommunicationException("AppointmentRecords", "getAppointmentRecord", "Unauthorized to access appointment record data", e);
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("AppointmentRecords", "getAppointmentRecord", e.getMessage(), e);
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAppointmentRecordsByPhysician(String physicianId) {
        String url = appointmentRecordsServiceUrl + "/appointment-records/physician/" + physicianId;
        try {
            return restTemplate.getForObject(url, List.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new MicroserviceCommunicationException("AppointmentRecords", "getAppointmentRecordsByPhysician", "Unauthorized to access appointment record data", e);
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("AppointmentRecords", "getAppointmentRecordsByPhysician", e.getMessage(), e);
        }
    }

    // ===== PEER MANAGEMENT =====

    public List<String> getPeerUrls() {
        return peers.stream()
                .filter(peerUrl -> !isCurrentInstance(peerUrl))
                .collect(Collectors.toList());
    }
    private boolean isCurrentInstance(String peerUrl) {
        return peerUrl.contains(":" + currentPort);
    }
    public String getCurrentInstanceUrl() {
        return "http://localhost:" + currentPort;
    }
    public boolean hasPeers() {
        return !getPeerUrls().isEmpty();
    }
    public int getPeerCount() {
        return getPeerUrls().size();
    }
}

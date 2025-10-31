package leti_sisdis_6.happhysicians.services;

import leti_sisdis_6.happhysicians.exceptions.AppointmentRecordNotFoundException;
import leti_sisdis_6.happhysicians.exceptions.MicroserviceCommunicationException;
import leti_sisdis_6.happhysicians.exceptions.PatientNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    void init() {
        System.out.println("Physicians peers configured for port " + currentPort + ": " + peers);
        System.out.println("Active peers (excluding self): " + getPeerUrls());
    }

    // Helper: forward caller identity to downstream services
    private HttpHeaders buildForwardHeaders() {
        HttpHeaders headers = new HttpHeaders();
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String auth = req.getHeader("Authorization");
            String userId = req.getHeader("X-User-Id");
            String userRole = req.getHeader("X-User-Role");
            if (auth != null && !auth.isBlank()) headers.add("Authorization", auth);
            if (userId != null && !userId.isBlank()) headers.add("X-User-Id", userId);
            if (userRole != null && !userRole.isBlank()) headers.add("X-User-Role", userRole);
        }
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    // Patient Service calls
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public Map<String, Object> getPatientById(String patientId) {
        String url = patientsServiceUrl + "/patients/" + patientId;
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(buildForwardHeaders()),
                    new ParameterizedTypeReference<Map<String, Object>>(){});
            return resp.getBody();
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
            HttpHeaders headers = buildForwardHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(patientIds, headers),
                    new ParameterizedTypeReference<List<Map<String, Object>>>(){});
            return resp.getBody();
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
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(buildForwardHeaders()),
                    new ParameterizedTypeReference<Map<String, Object>>(){});
            return resp.getBody();
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
            HttpHeaders headers = buildForwardHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(token, headers),
                    new ParameterizedTypeReference<Map<String, Object>>(){});
            return resp.getBody();
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
            HttpHeaders headers = buildForwardHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(request, headers),
                    new ParameterizedTypeReference<Map<String, Object>>(){});
            return resp.getBody();
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
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(buildForwardHeaders()),
                    new ParameterizedTypeReference<Map<String, Object>>(){});
            return resp.getBody();
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
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(buildForwardHeaders()),
                    new ParameterizedTypeReference<List<Map<String, Object>>>(){});
            return resp.getBody();
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

    // ===== Appointment Records: read appointments owned by that service =====
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listAppointments() {
        String url = appointmentRecordsServiceUrl + "/api/appointments";
        try {
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(buildForwardHeaders()),
                    new ParameterizedTypeReference<List<Map<String, Object>>>(){});
            return resp.getBody();
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("AppointmentRecords", "listAppointments", e.getMessage(), e);
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public Map<String, Object> getAppointment(String appointmentId) {
        String url = appointmentRecordsServiceUrl + "/api/appointments/" + appointmentId;
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(buildForwardHeaders()),
                    new ParameterizedTypeReference<Map<String, Object>>(){});
            return resp.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new AppointmentRecordNotFoundException("Appointment not found: " + appointmentId, e);
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("AppointmentRecords", "getAppointment", e.getMessage(), e);
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listAppointmentsByPhysician(String physicianId) {
        String url = appointmentRecordsServiceUrl + "/api/appointments/physician/" + physicianId;
        try {
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(buildForwardHeaders()),
                    new ParameterizedTypeReference<List<Map<String, Object>>>(){});
            return resp.getBody();
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("AppointmentRecords", "listAppointmentsByPhysician", e.getMessage(), e);
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listAppointmentsByPatient(String patientId) {
        String url = appointmentRecordsServiceUrl + "/api/appointments/patient/" + patientId;
        try {
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(buildForwardHeaders()),
                    new ParameterizedTypeReference<List<Map<String, Object>>>(){});
            return resp.getBody();
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("AppointmentRecords", "listAppointmentsByPatient", e.getMessage(), e);
        }
    }

    // ===== Write operations: create, update, delete appointments =====
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public Map<String, Object> createAppointmentInRecords(Map<String, Object> appointmentData) {
        String url = appointmentRecordsServiceUrl + "/api/appointments";
        try {
            HttpHeaders headers = buildForwardHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(appointmentData, headers),
                    new ParameterizedTypeReference<Map<String, Object>>(){});
            return resp.getBody();
        } catch (HttpClientErrorException.Conflict e) {
            throw new MicroserviceCommunicationException("AppointmentRecords", "createAppointment", "Appointment already exists or time conflict", e);
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("AppointmentRecords", "createAppointment", e.getMessage(), e);
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public Map<String, Object> updateAppointmentInRecords(String appointmentId, Map<String, Object> appointmentData) {
        String url = appointmentRecordsServiceUrl + "/api/appointments/" + appointmentId;
        try {
            HttpHeaders headers = buildForwardHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.PUT, new HttpEntity<>(appointmentData, headers),
                    new ParameterizedTypeReference<Map<String, Object>>(){});
            return resp.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new AppointmentRecordNotFoundException("Appointment not found: " + appointmentId, e);
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("AppointmentRecords", "updateAppointment", e.getMessage(), e);
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public void deleteAppointmentInRecords(String appointmentId) {
        String url = appointmentRecordsServiceUrl + "/api/appointments/" + appointmentId;
        try {
            HttpHeaders headers = buildForwardHeaders();
            restTemplate.exchange(
                    url, HttpMethod.DELETE, new HttpEntity<>(headers),
                    Void.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new AppointmentRecordNotFoundException("Appointment not found: " + appointmentId, e);
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("AppointmentRecords", "deleteAppointment", e.getMessage(), e);
        }
    }
}

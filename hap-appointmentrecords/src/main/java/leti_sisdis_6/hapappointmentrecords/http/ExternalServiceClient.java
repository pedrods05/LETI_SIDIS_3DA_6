package leti_sisdis_6.hapappointmentrecords.http;

import leti_sisdis_6.hapappointmentrecords.exceptions.MicroserviceCommunicationException;
import leti_sisdis_6.hapappointmentrecords.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExternalServiceClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${hap.physicians.base-url:http://localhost:8081}")
    private String physiciansServiceUrl;

    @Value("${hap.patients.base-url:http://localhost:8082}")
    private String patientsServiceUrl;

    @Value("${hap.auth.base-url:http://localhost:8084}")
    private String authServiceUrl;

    @Value("${server.port:8083}")
    private String currentPort;

    // Hardcoded peer list for appointmentrecords instances
    private final List<String> peers = Arrays.asList(
            "http://localhost:8083",
            "http://localhost:8090"
    );

    @PostConstruct
    void init() {
        System.out.println("AppointmentRecords peers configured for port " + currentPort + ": " + peers);
        System.out.println("Active peers (excluding self): " + getPeerUrls());
    }

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

    // Physician Service calls
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public Map<String, Object> getPhysicianById(String physicianId) {
        String url = physiciansServiceUrl + "/physicians/" + physicianId;
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(buildForwardHeaders()),
                    new ParameterizedTypeReference<Map<String, Object>>(){}
            );
            return resp.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Physician not found with id: " + physicianId, e);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new MicroserviceCommunicationException("Physicians", "getPhysicianById", "Unauthorized to access physician data", e);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new MicroserviceCommunicationException("Physicians", "getPhysicianById", "Forbidden to access physician data", e);
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("Physicians", "getPhysicianById", e.getMessage(), e);
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public Map<String, Object> getAppointmentById(String appointmentId) {
        String url = physiciansServiceUrl + "/appointments/" + appointmentId;
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(buildForwardHeaders()),
                    new ParameterizedTypeReference<Map<String, Object>>(){}
            );
            Map<String, Object> body = resp.getBody();
            if (body == null) {
                throw new NotFoundException("Appointment not found with id: " + appointmentId);
            }
            // Normalize: some responses embed physician info as an object { physician: { physicianId: ... } }
            Object directPhysicianId = body.get("physicianId");
            if (directPhysicianId == null) {
                Object physicianObj = body.get("physician");
                if (physicianObj instanceof Map<?, ?> physMap) {
                    Object nestedId = physMap.get("physicianId");
                    if (nestedId instanceof String pid && !pid.isBlank()) {
                        java.util.Map<String, Object> normalized = new java.util.HashMap<>(body);
                        normalized.put("physicianId", pid);
                        return normalized;
                    }
                }
            }
            return body;
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Appointment not found with id: " + appointmentId, e);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new MicroserviceCommunicationException("Physicians", "getAppointmentById", "Unauthorized to access appointment data", e);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new MicroserviceCommunicationException("Physicians", "getAppointmentById", "Forbidden to access appointment data", e);
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("Physicians", "getAppointmentById", e.getMessage(), e);
        }
    }

    // Patient Service calls
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public Map<String, Object> getPatientById(String patientId) {
        String url = patientsServiceUrl + "/patients/" + patientId;
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(buildForwardHeaders()),
                    new ParameterizedTypeReference<Map<String, Object>>(){}
            );
            return resp.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Patient not found with id: " + patientId, e);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new MicroserviceCommunicationException("Patients", "getPatientById", "Unauthorized to access patient data", e);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new MicroserviceCommunicationException("Patients", "getPatientById", "Forbidden to access patient data", e);
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("Patients", "getPatientById", e.getMessage(), e);
        }
    }

    // Auth Service calls
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public Map<String, Object> validateToken(String token) {
        String url = authServiceUrl + "/api/auth/validate";
        try {
            return restTemplate.getForObject(url + "?token=" + token, Map.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new MicroserviceCommunicationException("Auth", "validateToken", "Invalid token", e);
        } catch (Exception e) {
            throw new MicroserviceCommunicationException("Auth", "validateToken", e.getMessage(), e);
        }
    }

    // Peer communication methods
    public java.util.List<String> getPeerUrls() {
        return peers.stream()
                .filter(peerUrl -> !isCurrentInstance(peerUrl))
                .collect(Collectors.toList());
    }

    private boolean isCurrentInstance(String peerUrl) {
        return peerUrl.contains(":" + currentPort);
    }
}
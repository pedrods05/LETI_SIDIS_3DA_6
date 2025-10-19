package leti_sisdis_6.hapappointmentrecords.http;

import leti_sisdis_6.hapappointmentrecords.exceptions.MicroserviceCommunicationException;
import leti_sisdis_6.hapappointmentrecords.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

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

    // Physician Service calls
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public Map<String, Object> getPhysicianById(String physicianId) {
        String url = physiciansServiceUrl + "/physicians/" + physicianId;
        try {
            return restTemplate.getForObject(url, Map.class);
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
            return restTemplate.getForObject(url, Map.class);
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
            return restTemplate.getForObject(url, Map.class);
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
}

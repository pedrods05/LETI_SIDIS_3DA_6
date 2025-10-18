package leti_sisdis_6.happhysicians.services;

import leti_sisdis_6.happhysicians.dto.external.AppointmentRecordDTO;
import leti_sisdis_6.happhysicians.dto.external.PatientDTO;
import leti_sisdis_6.happhysicians.dto.external.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class ExternalServiceClient {

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Value("${hap.patients.base-url:http://localhost:8082}")
    private String patientsServiceUrl;

    @Value("${hap.auth.base-url:http://localhost:8080}")
    private String authServiceUrl;

    @Value("${hap.appointmentrecords.base-url:http://localhost:8083}")
    private String appointmentRecordsServiceUrl;

    private RestTemplate getRestTemplate() {
        if (this.restTemplate == null) {
            this.restTemplate = new RestTemplate();
        }
        return this.restTemplate;
    }

    // Patient Service calls
    public PatientDTO getPatientById(String patientId) {
        String url = patientsServiceUrl + "/patients/" + patientId;
        try {
            return getRestTemplate().getForObject(url, PatientDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Error calling Patients service during getPatientById: " + e.getMessage(), e);
        }
    }

    public List<PatientDTO> getPatientsByIds(List<String> patientIds) {
        String url = patientsServiceUrl + "/patients/batch";
        try {
            return getRestTemplate().postForObject(url, patientIds, List.class);
        } catch (Exception e) {
            throw new RuntimeException("Error calling Patients service during getPatientsByIds: " + e.getMessage(), e);
        }
    }

    // Auth Service calls
    public UserDTO getUserById(String userId) {
        String url = authServiceUrl + "/users/" + userId;
        try {
            return getRestTemplate().getForObject(url, UserDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Error calling Auth service during getUserById: " + e.getMessage(), e);
        }
    }

    public UserDTO validateToken(String token) {
        String url = authServiceUrl + "/auth/validate";
        try {
            return getRestTemplate().postForObject(url, token, UserDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Error calling Auth service during validateToken: " + e.getMessage(), e);
        }
    }

    // Appointment Records Service calls
    public AppointmentRecordDTO getAppointmentRecord(String appointmentId) {
        String url = appointmentRecordsServiceUrl + "/appointment-records/appointment/" + appointmentId;
        try {
            return getRestTemplate().getForObject(url, AppointmentRecordDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Error calling AppointmentRecords service during getAppointmentRecord: " + e.getMessage(), e);
        }
    }

    public List<AppointmentRecordDTO> getAppointmentRecordsByPhysician(String physicianId) {
        String url = appointmentRecordsServiceUrl + "/appointment-records/physician/" + physicianId;
        try {
            return getRestTemplate().getForObject(url, List.class);
        } catch (Exception e) {
            throw new RuntimeException("Error calling AppointmentRecords service during getAppointmentRecordsByPhysician: " + e.getMessage(), e);
        }
    }
}

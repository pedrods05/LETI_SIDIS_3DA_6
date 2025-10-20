package leti_sisdis_6.happatients.service;

import leti_sisdis_6.happatients.exceptions.EmailAlreadyExistsException;
import leti_sisdis_6.happatients.model.Address;
import leti_sisdis_6.happatients.model.InsuranceInfo;
import leti_sisdis_6.happatients.model.Patient;
import leti_sisdis_6.happatients.model.Photo;
import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import leti_sisdis_6.happatients.dto.PatientRegistrationDTO;
import leti_sisdis_6.happatients.dto.ContactDetailsUpdateDTO;
import leti_sisdis_6.happatients.api.PatientMapper;
import leti_sisdis_6.happatients.repository.PatientRepository;
import leti_sisdis_6.happatients.repository.PhotoRepository;
import leti_sisdis_6.happatients.repository.AddressRepository;
import leti_sisdis_6.happatients.repository.InsuranceInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import leti_sisdis_6.happatients.dto.PatientProfileDTO;
import leti_sisdis_6.happatients.http.ResilientRestTemplate;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository patientRepository;
    private final PhotoRepository photoRepository;
    private final AddressRepository addressRepository;
    private final InsuranceInfoRepository insuranceInfoRepository;
    private final PatientMapper patientMapper;
    private final ResilientRestTemplate resilientRestTemplate;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    // Each service should only know external services as a single base URL (no peer lists here)
    @Value("${hap.auth.base-url:http://localhost:8084}")
    private String authBaseUrl;

    @Value("${hap.physicians.base-url:http://localhost:8081}")
    private String physiciansBaseUrl;

    @Value("${hap.appointmentrecords.base-url:http://localhost:8083}")
    private String appointmentRecordsBaseUrl;

    private RestTemplate getRestTemplate() {
        if (this.restTemplate == null) {
            this.restTemplate = new RestTemplate();
        }
        return this.restTemplate;
    }

    @Transactional
    public String registerPatient(PatientRegistrationDTO dto) {
        if (!dto.getDataConsentGiven()) {
            throw new IllegalArgumentException("Data consent is required");
        }
        if (patientRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use");
        }
        String patientId = generatePatientId();

        // Create auth user via hap-auth API (single base URL)
        var req = new java.util.HashMap<String, String>();
        req.put("username", dto.getEmail());
        req.put("password", dto.getPassword());
        req.put("role", "PATIENT");
        try {
            ResponseEntity<Object> resp = getRestTemplate().postForEntity(
                    authBaseUrl + "/api/public/register",
                    req,
                    Object.class
            );
            if (!(resp.getStatusCode().is2xxSuccessful() || resp.getStatusCode().value() == 201)) {
                throw new IllegalArgumentException("Auth service responded with status: " + resp.getStatusCode());
            }
        } catch (HttpClientErrorException.Conflict e) { // 409
            throw new IllegalArgumentException("Username already exists");
        } catch (HttpClientErrorException.Unauthorized e) { // 401
            throw new IllegalArgumentException("Unauthorized calling auth service (401). Ensure /api/public/register is public.");
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to register user in auth service: " + e.getMessage());
        }

        Address address = Address.builder()
                .id(generateAddressId())
                .street(dto.getAddress().getStreet())
                .city(dto.getAddress().getCity())
                .postalCode(dto.getAddress().getPostalCode())
                .country(dto.getAddress().getCountry())
                .build();

        InsuranceInfo insuranceInfo = null;
        if (dto.getInsuranceInfo() != null) {
            insuranceInfo = InsuranceInfo.builder()
                    .id(generateInsuranceId())
                    .policyNumber(dto.getInsuranceInfo().getPolicyNumber())
                    .provider(dto.getInsuranceInfo().getProvider())
                    .coverageType(dto.getInsuranceInfo().getCoverageType())
                    .build();
        }

        Patient patient = Patient.builder()
                .patientId(patientId)
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .birthDate(dto.getBirthDate())
                .address(address)
                .insuranceInfo(insuranceInfo)
                .dataConsentGiven(true)
                .dataConsentDate(LocalDate.now())
                .build();

        Patient savedPatient = patientRepository.save(patient);
        return savedPatient.getPatientId();
    }

    @Transactional(readOnly = true)
    public PatientDetailsDTO getPatientDetails(String patientId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + patientId));
        return patientMapper.toDetailsDTO(patient);
    }

    @Transactional(readOnly = true)
    public List<PatientDetailsDTO> searchPatientsByName(String name) {
        List<Patient> patients = patientRepository.findByFullNameContainingIgnoreCase(name);
        if (patients.isEmpty()) {
            throw new EntityNotFoundException("No patients found with name containing: " + name);
        }
        return patients.stream().map(patientMapper::toDetailsDTO).toList();
    }

    @Transactional
    public String updateContactDetails(String email, ContactDetailsUpdateDTO dto) {
        Patient patient = patientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found with email: " + email));
        if (dto.getPhoneNumber() != null) {
            patient.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getAddress() != null) {
            patient.getAddress().setStreet(dto.getAddress().getStreet());
            patient.getAddress().setCity(dto.getAddress().getCity());
            patient.getAddress().setPostalCode(dto.getAddress().getPostalCode());
            patient.getAddress().setCountry(dto.getAddress().getCountry());
        }
        if (dto.getPhoto() != null) {
            Photo photo = Photo.builder()
                    .id(patient.getPhoto() != null ? patient.getPhoto().getId() : null)
                    .url(dto.getPhoto().getUrl())
                    .uploadedAt(LocalDateTime.parse(dto.getPhoto().getUploadedAt(), DateTimeFormatter.ISO_DATE_TIME))
                    .build();
            photo = photoRepository.save(photo);
            patient.setPhoto(photo);
        }
        patientRepository.save(patient);
        return "Contact details updated successfully.";
    }

    @Transactional(readOnly = true)
    public List<JsonNode> getPatientAppointmentHistory(String patientId, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, bearerToken.startsWith("Bearer ") ? bearerToken : ("Bearer " + bearerToken));
        }
        JsonNode[] body = tryGet(appointmentRecordsBaseUrl, "/api/appointment-records/patient/" + patientId, headers, JsonNode[].class);
        return body != null ? java.util.Arrays.asList(body) : java.util.Collections.emptyList();
    }

    @Transactional(readOnly = true)
    public List<JsonNode> getMyAppointmentHistory(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, bearerToken.startsWith("Bearer ") ? bearerToken : ("Bearer " + bearerToken));
        }
        JsonNode[] body = tryGet(appointmentRecordsBaseUrl, "/api/appointment-records/patient/mine", headers, JsonNode[].class);
        return body != null ? java.util.Arrays.asList(body) : java.util.Collections.emptyList();
    }

    @Transactional(readOnly = true)
    public JsonNode getPhysicianById(String physicianId) {
        return tryGet(physiciansBaseUrl, "/physicians/" + physicianId, null, JsonNode.class);
    }

    @Transactional(readOnly = true)
    public PatientProfileDTO getPatientProfile(String patientId, String bearerToken) {
        PatientDetailsDTO details = getPatientDetails(patientId);
        List<JsonNode> history = getPatientAppointmentHistory(patientId, bearerToken);
        return PatientProfileDTO.builder().patient(details).appointmentHistory(history).build();
    }

    @Transactional(readOnly = true)
    public PatientProfileDTO getMyProfile(String email, String bearerToken) {
        Patient patient = patientRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with email: " + email));
        PatientDetailsDTO details = patientMapper.toDetailsDTO(patient);
        List<JsonNode> history = getMyAppointmentHistory(bearerToken);
        return PatientProfileDTO.builder().patient(details).appointmentHistory(history).build();
    }

    @Transactional(readOnly = true)
    public List<PatientDetailsDTO> listAllPatients() {
        return patientRepository.findAll().stream().map(patientMapper::toDetailsDTO).toList();
    }

    private String generatePatientId() {
        List<String> existingIds = patientRepository.findAll().stream()
                .map(Patient::getPatientId)
                .filter(id -> id != null && id.startsWith("PAT"))
                .toList();
        int max = existingIds.stream()
                .map(id -> id.substring(3))
                .filter(s -> s.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        return String.format("PAT%02d", max + 1);
    }

    private String generateAddressId() {
        long count = addressRepository.count();
        return String.format("ADR%02d", count + 1);
    }

    private String generateInsuranceId() {
        long count = insuranceInfoRepository.count();
        return String.format("INS%02d", count + 1);
    }

    // Helper: GET from a single base URL with optional headers using resilient client
    private <T> T tryGet(String baseUrl, String path, HttpHeaders headers, Class<T> clazz) {
        String url = baseUrl + (path.startsWith("/") ? path : ("/" + path));
        try {
            if (headers != null) {
                return resilientRestTemplate.getForObjectWithFallback(url, headers, clazz);
            } else {
                return resilientRestTemplate.getForObjectWithFallback(url, clazz);
            }
        } catch (Exception ignored) {
            return null; // surface as empty/null to caller
        }
    }
}

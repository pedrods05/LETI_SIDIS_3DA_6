package leti_sisdis_6.happatients.service;

import leti_sisdis_6.happatients.exceptions.EmailAlreadyExistsException;
import leti_sisdis_6.happatients.exceptions.NotFoundException;
import leti_sisdis_6.happatients.model.*;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;

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

    @Value("${hap.appointmentrecords.base-url:http://localhost:8083}")
    private String appointmentRecordsBaseUrl;

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
    public PatientProfileDTO getPatientProfile(String id, String authorizationHeader) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found with ID: " + id));


        return PatientProfileDTO.builder()
                .patientId(patient.getPatientId())
                .fullName(patient.getFullName())
                .email(patient.getEmail())
                .phoneNumber(patient.getPhoneNumber())
                .birthDate(patient.getBirthDate())
                .dataConsentGiven(patient.isDataConsentGiven())
                .dataConsentDate(patient.getDataConsentDate())

                .address(PatientProfileDTO.AddressDTO.builder()
                        .street(patient.getAddress().getStreet())
                        .city(patient.getAddress().getCity())
                        .postalCode(patient.getAddress().getPostalCode())
                        .country(patient.getAddress().getCountry())
                        .build())

                .insuranceInfo(PatientProfileDTO.InsuranceInfoDTO.builder()
                        .policyNumber(patient.getInsuranceInfo().getPolicyNumber())
                        .provider(patient.getInsuranceInfo().getProvider())
                        .coverageType(patient.getInsuranceInfo().getCoverageType())
                        .build())

                .healthConcerns(patient.getHealthConcerns() != null
                        ? patient.getHealthConcerns().stream()
                        .map(HealthConcern::getDescription)
                        .toList()
                        : Collections.emptyList())

                .build();

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

    private <T> T tryGet(String baseUrl, String path, HttpHeaders headers, Class<T> clazz) {
        String url = baseUrl + (path.startsWith("/") ? path : ("/" + path));
        try {
            if (headers != null) {
                return resilientRestTemplate.getForObjectWithFallback(url, headers, clazz);
            } else {
                return resilientRestTemplate.getForObjectWithFallback(url, clazz);
            }
        } catch (Exception ignored) {
            return null;
        }
    }
}

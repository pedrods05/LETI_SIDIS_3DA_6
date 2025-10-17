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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository patientRepository;
    private final PhotoRepository photoRepository;
    private final AddressRepository addressRepository;
    private final InsuranceInfoRepository insuranceInfoRepository;
    private final PatientIdGenerator patientIdGenerator;
    private final AuthClient authClient;
    private final PatientMapper patientMapper;

    @Transactional
    public String registerPatient(PatientRegistrationDTO dto) {
        if (!dto.getDataConsentGiven()) {
            throw new IllegalArgumentException("Data consent is required");
        }

        if (patientRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use");
        }

        // Register user in AUTH service and use returned id as patient id
        String patientId = authClient.registerUser(dto.getEmail(), dto.getPassword(), "PATIENT").getId();

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

        return patients.stream()
                .map(patientMapper::toDetailsDTO)
                .toList();
    }

    @Transactional
    public String updateContactDetails(String email, ContactDetailsUpdateDTO dto) {
        System.out.println("Searching for patient with email: " + email);
        // Get patient from email
        Patient patient = patientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found with email: " + email));

        // Update phone number if provided
        if (dto.getPhoneNumber() != null) {
            patient.setPhoneNumber(dto.getPhoneNumber());
        }

        // Update address if provided
        if (dto.getAddress() != null) {
            patient.getAddress().setStreet(dto.getAddress().getStreet());
            patient.getAddress().setCity(dto.getAddress().getCity());
            patient.getAddress().setPostalCode(dto.getAddress().getPostalCode());
            patient.getAddress().setCountry(dto.getAddress().getCountry());
        }

        // Update photo if provided
        if (dto.getPhoto() != null) {
            Photo photo = Photo.builder()
                    .id(patient.getPhoto().getId())
                    .url(dto.getPhoto().getUrl())
                    .uploadedAt(LocalDateTime.parse(dto.getPhoto().getUploadedAt(), 
                            DateTimeFormatter.ISO_DATE_TIME))
                    .build();
            photo = photoRepository.save(photo);
            patient.setPhoto(photo);
        }

        patientRepository.save(patient);
        return "Contact details updated successfully.";
    }

    private String generatePatientId() {
        return patientIdGenerator.generateNextPatientId();
    }

    private String generateAddressId() {
        long count = addressRepository.count();
        return String.format("ADR%02d", count + 1);
    }

    private String generateInsuranceId() {
        long count = insuranceInfoRepository.count();
        return String.format("INS%02d", count + 1);
    }
}

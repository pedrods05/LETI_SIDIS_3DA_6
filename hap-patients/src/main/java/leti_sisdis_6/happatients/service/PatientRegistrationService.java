package leti_sisdis_6.happatients.service;

import leti_sisdis_6.happatients.exceptions.EmailAlreadyExistsException;
import leti_sisdis_6.happatients.dto.PatientRegistrationDTOV2;
import leti_sisdis_6.happatients.dto.HealthConcernDTO;
import leti_sisdis_6.happatients.model.*;
import leti_sisdis_6.happatients.repository.*;
import leti_sisdis_6.happatients.event.PatientRegisteredEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientRegistrationService {
    private final PatientRepository patientRepository;
    private final PhotoRepository photoRepository;
    private final AddressRepository addressRepository;
    private final InsuranceInfoRepository insuranceInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;
    private final PatientEventRepository patientEventRepository;


    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Value("${hap.auth.base-url:http://localhost:8084}")
    private String authServiceBaseUrl;

    @Value("${hap.rabbitmq.exchange:hap-exchange}")
    private String exchangeName;

    private RestTemplate getRestTemplate() {
        if (this.restTemplate == null) {
            this.restTemplate = new RestTemplate();
        }
        return this.restTemplate;
    }

    @Transactional
    public String registerPatient(PatientRegistrationDTOV2 dto) {
        log.info("Iniciando registo de novo paciente: {}", dto.getEmail());
        if (!dto.getDataConsentGiven()) {
            throw new IllegalArgumentException("Data consent is required");
        }

        if (patientRepository.existsByEmail(dto.getEmail())) {
            log.warn("Tentativa de registo com email duplicado: {}", dto.getEmail());
            throw new EmailAlreadyExistsException("Email address '" + dto.getEmail() + "' is already registered. Please use a different email address or try to recover your account if you forgot your password.");
        }

        if (dto.getHealthConcerns() != null) {
            validateHealthConcerns(dto.getHealthConcerns());
        }

        String patientId = generatePatientId();
        try {
            log.debug("A contactar Auth Service para criar credenciais...");
        Map<String, String> req = new HashMap<>();
        req.put("username", dto.getEmail());
        req.put("password", dto.getPassword());
        req.put("role", "PATIENT");

        getRestTemplate().postForEntity(
            authServiceBaseUrl + "/api/public/register",
            req,
            Object.class
        );
            log.debug("Credenciais criadas com sucesso no Auth Service.");
        } catch (Exception e) {
            log.error("Falha ao comunicar com Auth Service: {}", e.getMessage());
            throw e; // Relança para abortar a transação
        }



        Photo photo = Photo.builder()
                .id(generatePhotoId())
                .url(dto.getPhoto().getUrl())
                .uploadedAt(dto.getPhoto().getUploadedAt())
                .build();
        photo = photoRepository.save(photo);

        Address address = Address.builder()
                .id(generateAddressId())
                .street(dto.getAddress().getStreet())
                .city(dto.getAddress().getCity())
                .postalCode(dto.getAddress().getPostalCode())
                .country(dto.getAddress().getCountry())
                .build();

        InsuranceInfo insuranceInfo = InsuranceInfo.builder()
                .id(generateInsuranceId())
                .policyNumber(dto.getInsuranceInfo().getPolicyNumber())
                .provider(dto.getInsuranceInfo().getCompanyName())
                .coverageType(dto.getInsuranceInfo().getCoverageType())
                .build();

        List<HealthConcern> healthConcerns = new ArrayList<>();
        if (dto.getHealthConcerns() != null) {
            for (var concern : dto.getHealthConcerns()) {
                HealthConcern healthConcern = HealthConcern.builder()
                        .id(generateHealthConcernId())
                        .description(concern.getDescription())
                        .diagnosisDate(concern.getDiagnosisDate())
                        .treatment(concern.getTreatment())
                        .ongoing(concern.getOngoing())
                        .resolvedDate(concern.getResolvedDate())
                        .build();
                healthConcerns.add(healthConcern);
            }
        }

        Patient patient = Patient.builder()
                .patientId(patientId)
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .birthDate(dto.getBirthDate())
                .address(address)
                .insuranceInfo(insuranceInfo)
                .photo(photo)
                .healthConcerns(healthConcerns)
                .dataConsentGiven(true)
                .dataConsentDate(LocalDate.now())
                .build();

        patient = patientRepository.save(patient);
        log.info("Paciente persistido localmente (SQL). ID: {}", patient.getPatientId());

        // Audit event (event sourcing light)
        try {
            PatientEvent auditEvent = new PatientEvent(
                    "PATIENT_REGISTERED",
                    patient.getPatientId(),
                    "Patient registered with email=" + patient.getEmail(),
                    java.time.LocalDateTime.now()
            );
            patientEventRepository.save(auditEvent);
            log.info("[AUDIT] PatientEvent persistido para patientId={}", patient.getPatientId());
        } catch (Exception e) {
            log.error("[AUDIT] Falha ao persistir PatientEvent para patientId={}", patient.getPatientId(), e);
        }

        publishPatientRegisteredEvent(patient);
        return patient.getPatientId();
    }
    private void publishPatientRegisteredEvent(Patient patient) {

        try {
            PatientRegisteredEvent.AddressEventData addressData = new PatientRegisteredEvent.AddressEventData(
                    patient.getAddress().getStreet(),
                    patient.getAddress().getCity(),
                    patient.getAddress().getPostalCode(),
                    patient.getAddress().getCountry()
            );

            PatientRegisteredEvent.InsuranceEventData insuranceData = new PatientRegisteredEvent.InsuranceEventData(
                    patient.getInsuranceInfo().getPolicyNumber(),
                    patient.getInsuranceInfo().getProvider(),
                    patient.getInsuranceInfo().getCoverageType()
            );
            PatientRegisteredEvent event = new PatientRegisteredEvent(
                    patient.getPatientId(),
                    patient.getFullName(),
                    patient.getEmail(),
                    patient.getPhoneNumber(),
                    patient.getBirthDate(),
                    patient.isDataConsentGiven(),
                    patient.getDataConsentDate(),
                    addressData,
                    insuranceData
            );
            rabbitTemplate.convertAndSend(exchangeName, "patient.registered", event);

            log.info("⚡ Evento PatientRegisteredEvent enviado para o RabbitMQ | Exchange: {} | PatientID: {}", exchangeName, patient.getPatientId());

        } catch (Exception e) {
            log.error("⚠️ FALHA CRÍTICA ao enviar evento RabbitMQ para paciente {}", patient.getPatientId(), e);
        }
    }
    private void validateHealthConcerns(List<HealthConcernDTO> healthConcerns) {
        for (HealthConcernDTO concern : healthConcerns) {
            if (!concern.getOngoing() && concern.getResolvedDate() == null) {
                throw new IllegalArgumentException("Resolved date is required when health concern is not ongoing");
            }
        }
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

    private String generatePhotoId() {
        long count = photoRepository.count();
        return String.format("PHT%02d", count + 1);
    }

    private String generateHealthConcernId() {
        long count = patientRepository.count();
        return String.format("HCN%02d", count + 1);
    }
}

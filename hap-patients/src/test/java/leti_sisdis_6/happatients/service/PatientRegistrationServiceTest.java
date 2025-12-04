package leti_sisdis_6.happatients.service;

import leti_sisdis_6.happatients.dto.HealthConcernDTO;
import leti_sisdis_6.happatients.dto.PatientRegistrationDTOV2;
import leti_sisdis_6.happatients.exceptions.EmailAlreadyExistsException;
import leti_sisdis_6.happatients.model.*;
import leti_sisdis_6.happatients.repository.AddressRepository;
import leti_sisdis_6.happatients.repository.InsuranceInfoRepository;
import leti_sisdis_6.happatients.repository.PatientRepository;
import leti_sisdis_6.happatients.repository.PhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatientRegistrationServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private PhotoRepository photoRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private InsuranceInfoRepository insuranceInfoRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private PatientRegistrationService service;

    @BeforeEach
    void setup() {
        // Inject default base URL since @Value is not processed in plain unit tests
        ReflectionTestUtils.setField(service, "authServiceBaseUrl", "http://localhost:8084");
        // Ensure the service uses our mock instead of creating a real RestTemplate
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);

        // defaults for counts to drive ID generation (lenient to avoid unnecessary stubbing failures)
        lenient().when(addressRepository.count()).thenReturn(0L);
        lenient().when(insuranceInfoRepository.count()).thenReturn(0L);
        lenient().when(photoRepository.count()).thenReturn(0L);
        lenient().when(patientRepository.findAll()).thenReturn(List.of());
        lenient().when(passwordEncoder.encode(any())).thenReturn("ENC");
    }

    private PatientRegistrationDTOV2 validDto() {
        PatientRegistrationDTOV2 dto = new PatientRegistrationDTOV2();
        dto.setFullName("John Doe");
        dto.setEmail("john@example.com");
        dto.setPassword("ValidPass#123");
        dto.setPhoneNumber("+351912345678");
        dto.setBirthDate(LocalDate.of(1990, 1, 1));
        PatientRegistrationDTOV2.AddressDTO addr = new PatientRegistrationDTOV2.AddressDTO();
        addr.setStreet("S"); addr.setCity("C"); addr.setPostalCode("P"); addr.setCountry("PT");
        dto.setAddress(addr);
        PatientRegistrationDTOV2.InsuranceInfoDTO ins = new PatientRegistrationDTOV2.InsuranceInfoDTO();
        ins.setPolicyNumber("PN"); ins.setCompanyName("Comp"); ins.setCoverageType("Basic");
        dto.setInsuranceInfo(ins);
        dto.setDataConsentGiven(true);
        var photo = new leti_sisdis_6.happatients.dto.PhotoDTO();
        photo.setUrl("http://img"); photo.setUploadedAt(LocalDateTime.now());
        dto.setPhoto(photo);
        return dto;
    }

    @Test
    void register_ok_persistsEntities_andCallsAuth() {
        PatientRegistrationDTOV2 dto = validDto();
        when(patientRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(photoRepository.save(any(Photo.class))).thenAnswer(inv -> inv.getArgument(0));
        // avoid real HTTP; service uses getRestTemplate() which returns mocked restTemplate
        doReturn(ResponseEntity.ok().build()).when(restTemplate)
                .postForEntity(anyString(), any(), eq(Object.class));

        // capture saved patient
        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        when(patientRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        String pid = service.registerPatient(dto);
        assertThat(pid).startsWith("PAT");
        Patient saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
        assertThat(saved.getAddress().getCity()).isEqualTo("C");
        assertThat(saved.getInsuranceInfo().getProvider()).isEqualTo("Comp");
        verify(restTemplate, atLeastOnce()).postForEntity(contains("/api/public/register"), any(), eq(Object.class));
    }

    @Test
    void register_fails_whenConsentMissing() {
        PatientRegistrationDTOV2 dto = validDto();
        dto.setDataConsentGiven(false);
        assertThrows(IllegalArgumentException.class, () -> service.registerPatient(dto));
    }

    @Test
    void register_fails_whenEmailExists() {
        PatientRegistrationDTOV2 dto = validDto();
        when(patientRepository.existsByEmail(dto.getEmail())).thenReturn(true);
        assertThrows(EmailAlreadyExistsException.class, () -> service.registerPatient(dto));
    }

    @Test
    void register_fails_whenResolvedDateMissingOnNonOngoingConcern() {
        PatientRegistrationDTOV2 dto = validDto();
        HealthConcernDTO c = new HealthConcernDTO();
        c.setDescription("asthma");
        c.setOngoing(false);
        c.setDiagnosisDate(LocalDate.now().minusYears(1));
        // resolvedDate intentionally null
        dto.setHealthConcerns(List.of(c));

        assertThrows(IllegalArgumentException.class, () -> service.registerPatient(dto));
    }
}

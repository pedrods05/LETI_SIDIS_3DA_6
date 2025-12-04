package leti_sisdis_6.happatients.service;

import com.fasterxml.jackson.databind.JsonNode;
import leti_sisdis_6.happatients.api.PatientMapper;
import leti_sisdis_6.happatients.dto.ContactDetailsUpdateDTO;
import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import leti_sisdis_6.happatients.http.ResilientRestTemplate;
import leti_sisdis_6.happatients.model.Address;
import leti_sisdis_6.happatients.model.Patient;
import leti_sisdis_6.happatients.model.Photo;
import leti_sisdis_6.happatients.repository.AddressRepository;
import leti_sisdis_6.happatients.repository.InsuranceInfoRepository;
import leti_sisdis_6.happatients.repository.PatientRepository;
import leti_sisdis_6.happatients.repository.PhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private PhotoRepository photoRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private InsuranceInfoRepository insuranceInfoRepository;
    @Mock private ResilientRestTemplate resilientRestTemplate;
    private PatientMapper mapper;

    private PatientService service;

    @BeforeEach
    void init() {
        mapper = new PatientMapper();
        service = new PatientService(patientRepository, photoRepository, addressRepository, insuranceInfoRepository, mapper, resilientRestTemplate);
    }

    @Test
    void getPatientDetails_ok() {
        Patient p = Patient.builder()
                .patientId("PAT01").fullName("Alice").email("a@a")
                .birthDate(LocalDate.now()).phoneNumber("+351900000000")
                .dataConsentGiven(true).dataConsentDate(LocalDate.now())
                .address(Address.builder().id("ADR01").street("s").city("c").postalCode("p").country("ct").build())
                .build();
        when(patientRepository.findById("PAT01")).thenReturn(Optional.of(p));

        PatientDetailsDTO dto = service.getPatientDetails("PAT01");
        assertThat(dto.getFullName()).isEqualTo("Alice");
        assertThat(dto.getAddress().getCity()).isEqualTo("c");
    }

    @Test
    void searchPatientsByName_notFound_throws() {
        when(patientRepository.findByFullNameContainingIgnoreCase("x")).thenReturn(List.of());
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.searchPatientsByName("x"));
    }

    @Test
    void updateContactDetails_updatesPhoneAndAddressAndPhoto() {
        Patient p = Patient.builder()
                .patientId("PAT01").email("john@example.com")
                .fullName("John").birthDate(LocalDate.now()).phoneNumber("1")
                .dataConsentGiven(true).dataConsentDate(LocalDate.now())
                .address(Address.builder().id("ADR01").street("s").city("c").postalCode("p").country("ct").build())
                .build();
        when(patientRepository.findByEmail("john@example.com")).thenReturn(Optional.of(p));
        when(photoRepository.save(any(Photo.class))).thenAnswer(inv -> {
            Photo ph = inv.getArgument(0);
            if (ph.getId() == null) ph.setId("PHT01");
            return ph;
        });

        ContactDetailsUpdateDTO dto = new ContactDetailsUpdateDTO();
        dto.setPhoneNumber("+351912345678");
        ContactDetailsUpdateDTO.AddressDTO addr = new ContactDetailsUpdateDTO.AddressDTO();
        addr.setStreet("A"); addr.setCity("B"); addr.setPostalCode("C"); addr.setCountry("D");
        dto.setAddress(addr);
        ContactDetailsUpdateDTO.PhotoDTO photo = new ContactDetailsUpdateDTO.PhotoDTO();
        photo.setUrl("http://x"); photo.setUploadedAt(LocalDateTime.now().toString());
        dto.setPhoto(photo);

        String msg = service.updateContactDetails("john@example.com", dto);
        assertThat(msg).contains("successfully");
        assertThat(p.getPhoneNumber()).isEqualTo("+351912345678");
        assertThat(p.getAddress().getCity()).isEqualTo("B");
        verify(patientRepository).save(p);
    }

    @Test
    void getPatientAppointmentHistory_callsExternalGracefully() {
        when(resilientRestTemplate.getForObjectWithFallback(anyString(), any(org.springframework.http.HttpHeaders.class), eq(JsonNode[].class)))
                .thenReturn(null);
        List<JsonNode> res = service.getPatientAppointmentHistory("PAT01", "Bearer token");
        assertThat(res).isEmpty();
    }
}


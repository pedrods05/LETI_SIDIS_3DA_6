package leti_sisdis_6.happhysicians.api;

import leti_sisdis_6.happhysicians.dto.request.RegisterPhysicianRequest;
import leti_sisdis_6.happhysicians.dto.response.PhysicianFullDTO;
import leti_sisdis_6.happhysicians.dto.response.PhysicianLimitedDTO;
import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.model.Specialty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PhysicianMapperTest {

    private PhysicianMapper mapper;
    private Department testDepartment;
    private Specialty testSpecialty;
    private RegisterPhysicianRequest registerRequest;

    @BeforeEach
    void setUp() {
        mapper = new PhysicianMapper();

        testDepartment = Department.builder()
                .departmentId("DEPT01")
                .code("CARD")
                .name("Cardiology")
                .build();

        testSpecialty = Specialty.builder()
                .specialtyId("SPEC01")
                .name("Cardiologist")
                .build();

        registerRequest = new RegisterPhysicianRequest();
        registerRequest.setFullName("Dr. John Doe");
        registerRequest.setLicenseNumber("LIC123");
        registerRequest.setUsername("john.doe@hospital.com");
        registerRequest.setPassword("password123");
        registerRequest.setDepartmentId("DEPT01");
        registerRequest.setSpecialtyId("SPEC01");
        registerRequest.setEmails(Arrays.asList("john@example.com"));
        registerRequest.setPhoneNumbers(Arrays.asList("123456789"));
        registerRequest.setWorkingHourStart("09:00");
        registerRequest.setWorkingHourEnd("17:00");
    }

    @Test
    void testToEntity_Success() {
        // Act
        Physician result = mapper.toEntity(registerRequest, testDepartment, testSpecialty);

        // Assert
        assertNotNull(result);
        assertEquals("Dr. John Doe", result.getFullName());
        assertEquals("LIC123", result.getLicenseNumber());
        assertEquals("john.doe@hospital.com", result.getUsername());
        assertEquals(testDepartment, result.getDepartment());
        assertEquals(testSpecialty, result.getSpecialty());
        assertEquals(LocalTime.of(9, 0), result.getWorkingHourStart());
        assertEquals(LocalTime.of(17, 0), result.getWorkingHourEnd());
        assertNotNull(result.getEmails());
        assertNotNull(result.getPhoneNumbers());
    }

    @Test
    void testToFullDTO_Success() {
        // Arrange
        Physician physician = Physician.builder()
                .physicianId("PHY01")
                .fullName("Dr. John Doe")
                .licenseNumber("LIC123")
                .username("john.doe@hospital.com")
                .specialty(testSpecialty)
                .department(testDepartment)
                .emails(Arrays.asList("john@example.com"))
                .phoneNumbers(Arrays.asList("123456789"))
                .workingHourStart(LocalTime.of(9, 0))
                .workingHourEnd(LocalTime.of(17, 0))
                .build();

        // Act
        PhysicianFullDTO result = mapper.toFullDTO(physician);

        // Assert
        assertNotNull(result);
        assertEquals("PHY01", result.getPhysicianId());
        assertEquals("Dr. John Doe", result.getFullName());
        assertEquals("LIC123", result.getLicenseNumber());
        assertEquals("john.doe@hospital.com", result.getUsername());
        assertEquals("Cardiologist", result.getSpecialtyName());
        assertEquals("Cardiology", result.getDepartmentName());
        assertEquals(LocalTime.of(9, 0), result.getWorkingHourStart());
        assertEquals(LocalTime.of(17, 0), result.getWorkingHourEnd());
    }

    @Test
    void testToFullDTO_WithPhoto() {
        // Arrange
        Physician.PhotoInfo photoInfo = new Physician.PhotoInfo();
        photoInfo.setUrl("/photos/photo.jpg");
        photoInfo.setUploadedAt(LocalDateTime.now());

        Physician physician = Physician.builder()
                .physicianId("PHY01")
                .fullName("Dr. John Doe")
                .licenseNumber("LIC123")
                .username("john.doe@hospital.com")
                .specialty(testSpecialty)
                .department(testDepartment)
                .photo(photoInfo)
                .workingHourStart(LocalTime.of(9, 0))
                .workingHourEnd(LocalTime.of(17, 0))
                .build();

        // Act
        PhysicianFullDTO result = mapper.toFullDTO(physician);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getPhoto());
        assertEquals("/photos/photo.jpg", result.getPhoto().getUrl());
    }

    @Test
    void testToLimitedDTO_Success() {
        // Arrange
        Physician physician = Physician.builder()
                .physicianId("PHY01")
                .fullName("Dr. John Doe")
                .specialty(testSpecialty)
                .department(testDepartment)
                .emails(Arrays.asList("john@example.com"))
                .phoneNumbers(Arrays.asList("123456789"))
                .workingHourStart(LocalTime.of(9, 0))
                .workingHourEnd(LocalTime.of(17, 0))
                .build();

        // Act
        PhysicianLimitedDTO result = mapper.toLimitedDTO(physician);

        // Assert
        assertNotNull(result);
        assertEquals("PHY01", result.getPhysicianId());
        assertEquals("Dr. John Doe", result.getFullName());
        assertEquals("Cardiologist", result.getSpecialtyName());
        assertEquals("Cardiology", result.getDepartmentName());
        assertEquals(LocalTime.of(9, 0), result.getWorkingHourStart());
        assertEquals(LocalTime.of(17, 0), result.getWorkingHourEnd());
    }

    @Test
    void testToLimitedDTOList_Success() {
        // Arrange
        Physician physician1 = Physician.builder()
                .physicianId("PHY01")
                .fullName("Dr. John Doe")
                .specialty(testSpecialty)
                .department(testDepartment)
                .workingHourStart(LocalTime.of(9, 0))
                .workingHourEnd(LocalTime.of(17, 0))
                .build();

        Physician physician2 = Physician.builder()
                .physicianId("PHY02")
                .fullName("Dr. Jane Smith")
                .specialty(testSpecialty)
                .department(testDepartment)
                .workingHourStart(LocalTime.of(10, 0))
                .workingHourEnd(LocalTime.of(18, 0))
                .build();

        List<Physician> physicians = Arrays.asList(physician1, physician2);

        // Act
        List<PhysicianLimitedDTO> result = mapper.toLimitedDTOList(physicians);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("PHY01", result.get(0).getPhysicianId());
        assertEquals("PHY02", result.get(1).getPhysicianId());
    }

    @Test
    void testToLimitedDTOList_EmptyList() {
        // Arrange
        List<Physician> physicians = List.of();

        // Act
        List<PhysicianLimitedDTO> result = mapper.toLimitedDTOList(physicians);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}


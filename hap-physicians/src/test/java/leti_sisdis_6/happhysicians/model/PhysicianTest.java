package leti_sisdis_6.happhysicians.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PhysicianTest {

    private Physician physician;
    private Department testDepartment;
    private Specialty testSpecialty;

    @BeforeEach
    void setUp() {
        testDepartment = Department.builder()
                .departmentId("DEPT01")
                .code("CARD")
                .name("Cardiology")
                .build();

        testSpecialty = Specialty.builder()
                .specialtyId("SPEC01")
                .name("Cardiologist")
                .build();

        physician = new Physician();
    }

    @Test
    void testNoArgsConstructor() {
        assertNotNull(physician);
        assertNull(physician.getPhysicianId());
        assertNull(physician.getFullName());
        assertNull(physician.getLicenseNumber());
        assertNull(physician.getUsername());
        assertNull(physician.getPassword());
        assertNull(physician.getSpecialty());
        assertNull(physician.getDepartment());
        assertNull(physician.getEmails());
        assertNull(physician.getPhoneNumbers());
        assertNull(physician.getWorkingHourStart());
        assertNull(physician.getWorkingHourEnd());
        assertNull(physician.getPhoto());
    }

    @Test
    void testAllArgsConstructor() {
        List<String> emails = Arrays.asList("doc1@hospital.com", "doc1@clinic.com");
        List<String> phones = Arrays.asList("123456789", "987654321");

        Physician doc = new Physician(
                "PHY01",
                "Dr. John Doe",
                "LIC123",
                "john.doe@hospital.com",
                "encodedPassword",
                testSpecialty,
                testDepartment,
                emails,
                phones,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                null
        );

        assertEquals("PHY01", doc.getPhysicianId());
        assertEquals("Dr. John Doe", doc.getFullName());
        assertEquals("LIC123", doc.getLicenseNumber());
        assertEquals("john.doe@hospital.com", doc.getUsername());
        assertEquals("encodedPassword", doc.getPassword());
        assertEquals(testSpecialty, doc.getSpecialty());
        assertEquals(testDepartment, doc.getDepartment());
        assertEquals(emails, doc.getEmails());
        assertEquals(phones, doc.getPhoneNumbers());
        assertEquals(LocalTime.of(9, 0), doc.getWorkingHourStart());
        assertEquals(LocalTime.of(17, 0), doc.getWorkingHourEnd());
        assertNull(doc.getPhoto());
    }

    @Test
    void testBuilder() {
        List<String> emails = Arrays.asList("doc2@hospital.com");
        List<String> phones = Arrays.asList("111222333");

        Physician.PhotoInfo photoInfo = new Physician.PhotoInfo(
                "/photos/PHY02.jpg",
                LocalDateTime.now()
        );

        Physician doc = Physician.builder()
                .physicianId("PHY02")
                .fullName("Dr. Jane Smith")
                .licenseNumber("LIC456")
                .username("jane.smith@hospital.com")
                .password("encodedPassword2")
                .specialty(testSpecialty)
                .department(testDepartment)
                .emails(emails)
                .phoneNumbers(phones)
                .workingHourStart(LocalTime.of(8, 0))
                .workingHourEnd(LocalTime.of(16, 0))
                .photo(photoInfo)
                .build();

        assertEquals("PHY02", doc.getPhysicianId());
        assertEquals("Dr. Jane Smith", doc.getFullName());
        assertEquals("LIC456", doc.getLicenseNumber());
        assertEquals("jane.smith@hospital.com", doc.getUsername());
        assertEquals("encodedPassword2", doc.getPassword());
        assertEquals(testSpecialty, doc.getSpecialty());
        assertEquals(testDepartment, doc.getDepartment());
        assertEquals(emails, doc.getEmails());
        assertEquals(phones, doc.getPhoneNumbers());
        assertEquals(LocalTime.of(8, 0), doc.getWorkingHourStart());
        assertEquals(LocalTime.of(16, 0), doc.getWorkingHourEnd());
        assertNotNull(doc.getPhoto());
        assertEquals("/photos/PHY02.jpg", doc.getPhoto().getUrl());
    }

    @Test
    void testSettersAndGetters() {
        List<String> emails = Arrays.asList("test@hospital.com");
        List<String> phones = Arrays.asList("999888777");

        physician.setPhysicianId("PHY03");
        physician.setFullName("Dr. Bob Johnson");
        physician.setLicenseNumber("LIC789");
        physician.setUsername("bob.johnson@hospital.com");
        physician.setPassword("encodedPassword3");
        physician.setSpecialty(testSpecialty);
        physician.setDepartment(testDepartment);
        physician.setEmails(emails);
        physician.setPhoneNumbers(phones);
        physician.setWorkingHourStart(LocalTime.of(10, 0));
        physician.setWorkingHourEnd(LocalTime.of(18, 0));

        assertEquals("PHY03", physician.getPhysicianId());
        assertEquals("Dr. Bob Johnson", physician.getFullName());
        assertEquals("LIC789", physician.getLicenseNumber());
        assertEquals("bob.johnson@hospital.com", physician.getUsername());
        assertEquals("encodedPassword3", physician.getPassword());
        assertEquals(testSpecialty, physician.getSpecialty());
        assertEquals(testDepartment, physician.getDepartment());
        assertEquals(emails, physician.getEmails());
        assertEquals(phones, physician.getPhoneNumbers());
        assertEquals(LocalTime.of(10, 0), physician.getWorkingHourStart());
        assertEquals(LocalTime.of(18, 0), physician.getWorkingHourEnd());
    }

    @Test
    void testPhotoInfo() {
        LocalDateTime uploadedAt = LocalDateTime.now();
        Physician.PhotoInfo photoInfo = new Physician.PhotoInfo("/photos/test.jpg", uploadedAt);

        assertEquals("/photos/test.jpg", photoInfo.getUrl());
        assertEquals(uploadedAt, photoInfo.getUploadedAt());

        // Test setters
        photoInfo.setUrl("/photos/new.jpg");
        photoInfo.setUploadedAt(LocalDateTime.now().plusDays(1));

        assertEquals("/photos/new.jpg", photoInfo.getUrl());
        assertNotNull(photoInfo.getUploadedAt());
    }

    @Test
    void testPhotoInfo_NoArgsConstructor() {
        Physician.PhotoInfo photoInfo = new Physician.PhotoInfo();
        assertNull(photoInfo.getUrl());
        assertNull(photoInfo.getUploadedAt());
    }

    @Test
    void testEmailsList() {
        List<String> emails1 = Arrays.asList("email1@hospital.com");
        physician.setEmails(emails1);
        assertEquals(1, physician.getEmails().size());
        assertEquals("email1@hospital.com", physician.getEmails().get(0));

        List<String> emails2 = Arrays.asList("email1@hospital.com", "email2@clinic.com");
        physician.setEmails(emails2);
        assertEquals(2, physician.getEmails().size());
        assertTrue(physician.getEmails().contains("email1@hospital.com"));
        assertTrue(physician.getEmails().contains("email2@clinic.com"));
    }

    @Test
    void testPhoneNumbersList() {
        List<String> phones1 = Arrays.asList("123456789");
        physician.setPhoneNumbers(phones1);
        assertEquals(1, physician.getPhoneNumbers().size());
        assertEquals("123456789", physician.getPhoneNumbers().get(0));

        List<String> phones2 = Arrays.asList("123456789", "987654321");
        physician.setPhoneNumbers(phones2);
        assertEquals(2, physician.getPhoneNumbers().size());
        assertTrue(physician.getPhoneNumbers().contains("123456789"));
        assertTrue(physician.getPhoneNumbers().contains("987654321"));
    }

    @Test
    void testWorkingHours() {
        physician.setWorkingHourStart(LocalTime.of(9, 30));
        physician.setWorkingHourEnd(LocalTime.of(17, 30));

        assertEquals(LocalTime.of(9, 30), physician.getWorkingHourStart());
        assertEquals(LocalTime.of(17, 30), physician.getWorkingHourEnd());
    }

    @Test
    void testBuilder_WithNullCollections() {
        Physician doc = Physician.builder()
                .physicianId("PHY04")
                .fullName("Dr. Test")
                .licenseNumber("LIC999")
                .username("test@hospital.com")
                .password("password")
                .specialty(testSpecialty)
                .department(testDepartment)
                .workingHourStart(LocalTime.of(9, 0))
                .workingHourEnd(LocalTime.of(17, 0))
                .build();

        assertNull(doc.getEmails());
        assertNull(doc.getPhoneNumbers());
        assertNull(doc.getPhoto());
    }
}


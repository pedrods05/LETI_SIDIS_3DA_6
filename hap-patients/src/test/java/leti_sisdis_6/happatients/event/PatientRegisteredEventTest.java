package leti_sisdis_6.happatients.event;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PatientRegisteredEventTest {

    @Test
    void lombokGeneratedMethods_shouldWorkForTopLevelEvent() {
        // given
        PatientRegisteredEvent.AddressEventData address =
                new PatientRegisteredEvent.AddressEventData("Street", "City", "1234-567", "PT");
        PatientRegisteredEvent.InsuranceEventData insurance =
                new PatientRegisteredEvent.InsuranceEventData("POL123", "Provider", "FULL");

        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        LocalDate consentDate = LocalDate.of(2024, 1, 1);

        // when
        PatientRegisteredEvent event = new PatientRegisteredEvent(
                "PAT01",
                "John Doe",
                "john.doe@example.com",
                "+351912345678",
                birthDate,
                true,
                consentDate,
                address,
                insurance
        );

        // then - getters (and constructor wiring)
        assertEquals("PAT01", event.getPatientId());
        assertEquals("John Doe", event.getFullName());
        assertEquals("john.doe@example.com", event.getEmail());
        assertEquals("+351912345678", event.getPhoneNumber());
        assertEquals(birthDate, event.getBirthDate());
        assertTrue(event.getDataConsentGiven());
        assertEquals(consentDate, event.getDataConsentDate());
        assertSame(address, event.getAddress());
        assertSame(insurance, event.getInsuranceInfo());

        // equals / hashCode should consider all fields, so two equal instances are equal
        PatientRegisteredEvent same = new PatientRegisteredEvent(
                "PAT01",
                "John Doe",
                "john.doe@example.com",
                "+351912345678",
                birthDate,
                true,
                consentDate,
                address,
                insurance
        );

        assertEquals(event, same);
        assertEquals(event.hashCode(), same.hashCode());

        // toString should contain at least the class name and a key field
        String toString = event.toString();
        assertTrue(toString.contains("PatientRegisteredEvent"));
        assertTrue(toString.contains("PAT01"));
    }

    @Test
    void noArgsConstructor_andSetters_shouldInitializeNestedTypes() {
        // when
        PatientRegisteredEvent.AddressEventData address = new PatientRegisteredEvent.AddressEventData();
        address.setStreet("Rua A");
        address.setCity("Porto");
        address.setPostalCode("4000-001");
        address.setCountry("PT");

        PatientRegisteredEvent.InsuranceEventData insurance = new PatientRegisteredEvent.InsuranceEventData();
        insurance.setPolicyNumber("POL999");
        insurance.setProvider("Seguradora");
        insurance.setCoverageType("BASIC");

        // then
        assertEquals("Rua A", address.getStreet());
        assertEquals("Porto", address.getCity());
        assertEquals("4000-001", address.getPostalCode());
        assertEquals("PT", address.getCountry());

        assertEquals("POL999", insurance.getPolicyNumber());
        assertEquals("Seguradora", insurance.getProvider());
        assertEquals("BASIC", insurance.getCoverageType());
    }
}


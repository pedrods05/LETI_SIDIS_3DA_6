package leti_sisdis_6.happhysicians.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentStatusTest {

    @Test
    void testFromString_ValidValues() {
        assertEquals(AppointmentStatus.SCHEDULED, AppointmentStatus.fromString("SCHEDULED"));
        assertEquals(AppointmentStatus.CANCELED, AppointmentStatus.fromString("CANCELED"));
        assertEquals(AppointmentStatus.COMPLETED, AppointmentStatus.fromString("COMPLETED"));
        
        // Case insensitive
        assertEquals(AppointmentStatus.SCHEDULED, AppointmentStatus.fromString("scheduled"));
        assertEquals(AppointmentStatus.CANCELED, AppointmentStatus.fromString("CANCELED"));
        assertEquals(AppointmentStatus.COMPLETED, AppointmentStatus.fromString("Completed"));
    }

    @Test
    void testFromString_InvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            AppointmentStatus.fromString("INVALID_STATUS");
        });
    }

    @Test
    void testFromString_Null() {
        assertThrows(IllegalArgumentException.class, () -> {
            AppointmentStatus.fromString(null);
        });
    }

    @Test
    void testFromString_EmptyString() {
        assertThrows(IllegalArgumentException.class, () -> {
            AppointmentStatus.fromString("");
        });
    }
}


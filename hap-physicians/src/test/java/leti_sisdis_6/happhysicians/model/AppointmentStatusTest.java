package leti_sisdis_6.happhysicians.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentStatusTest {

    @Test
    void testEnumValues() {
        // Verifica que todos os valores do enum existem
        AppointmentStatus[] values = AppointmentStatus.values();
        assertEquals(3, values.length);
        assertTrue(containsValue(values, AppointmentStatus.SCHEDULED));
        assertTrue(containsValue(values, AppointmentStatus.CANCELED));
        assertTrue(containsValue(values, AppointmentStatus.COMPLETED));
    }

    @Test
    void testGetValue() {
        assertEquals("SCHEDULED", AppointmentStatus.SCHEDULED.getValue());
        assertEquals("CANCELED", AppointmentStatus.CANCELED.getValue());
        assertEquals("COMPLETED", AppointmentStatus.COMPLETED.getValue());
    }

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

    private boolean containsValue(AppointmentStatus[] values, AppointmentStatus status) {
        for (AppointmentStatus value : values) {
            if (value == status) {
                return true;
            }
        }
        return false;
    }
}


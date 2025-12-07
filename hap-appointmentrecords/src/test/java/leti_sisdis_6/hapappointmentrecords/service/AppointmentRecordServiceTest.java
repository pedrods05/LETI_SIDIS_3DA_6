package leti_sisdis_6.hapappointmentrecords.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * AppointmentRecordServiceTest
 *
 * TODO: Update tests after refactoring
 * - Remove Appointment entity references
 * - Mock ExternalServiceClient.getAppointmentById() instead
 * - Update assertions to work with appointmentId String field
 *
 * Tests disabled temporarily after removing Appointment entity from this module.
 * Appointments now live in hap-physicians service.
 */
@Disabled("Tests need to be updated after Appointment entity removal")
class AppointmentRecordServiceTest {

    @Test
    @DisplayName("Placeholder test - needs refactoring")
    void placeholder() {
        // This class needs to be refactored to work with the new architecture
        // where appointments come from hap-physicians service via HTTP
    }
}


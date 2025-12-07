package leti_sisdis_6.hapappointmentrecords.setup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;

/**
 * DataBootstrap for appointment records.
 * Note: Appointments are NOT stored locally - they come from physicians service.
 * This bootstrap can be used to seed test appointment records if needed.
 */
@Component
@Profile("seed-appointments")
@RequiredArgsConstructor
@Order(2)
@Slf4j
public class DataBootstrap implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("DataBootstrap: No appointment seeding needed - appointments come from physicians service");
        // If you need to seed test appointment records, add logic here using appointmentIds from physicians service
    }
}

package leti_sisdis_6.hapappointmentrecords.setup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;

@Component
@Profile("seed-appointments")
@RequiredArgsConstructor
@Order(2)
@Slf4j
public class DataBootstrap implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("DataBootstrap: appointments come from physicians service");
    }
}

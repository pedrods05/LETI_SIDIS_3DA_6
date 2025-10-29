package leti_sisdis_6.hapappointmentrecords;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "leti_sisdis_6.hapappointmentrecords"
})
@EnableJpaRepositories(basePackages = {
        "leti_sisdis_6.hapappointmentrecords.repository"
})
@EntityScan(basePackages = {
        "leti_sisdis_6.hapappointmentrecords.model"
})
public class HapAppointmentrecordsApplication {
    public static void main(String[] args) {
        SpringApplication.run(HapAppointmentrecordsApplication.class, args);
    }
}

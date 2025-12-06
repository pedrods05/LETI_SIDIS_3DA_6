package leti_sisdis_6.hapappointmentrecords;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("leti_sisdis_6.hapappointmentrecords.repository")
public class HapAppointmentrecordsApplication {
    public static void main(String[] args) {
        SpringApplication.run(HapAppointmentrecordsApplication.class, args);
    }
}

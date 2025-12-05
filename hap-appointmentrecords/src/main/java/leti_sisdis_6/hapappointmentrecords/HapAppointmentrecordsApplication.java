package leti_sisdis_6.hapappointmentrecords;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableJpaRepositories("leti_sisdis_6.hapappointmentrecords.repository")
@EnableMongoRepositories("leti_sisdis_6.hapappointmentrecords.mongo")
public class HapAppointmentrecordsApplication {
    public static void main(String[] args) {
        SpringApplication.run(HapAppointmentrecordsApplication.class, args);
    }
}

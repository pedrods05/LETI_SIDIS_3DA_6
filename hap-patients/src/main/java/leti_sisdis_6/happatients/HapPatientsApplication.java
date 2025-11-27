package leti_sisdis_6.happatients;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "leti_sisdis_6.happatients.repository")
@EnableMongoRepositories(basePackages = "leti_sisdis_6.happatients.query")

public class HapPatientsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HapPatientsApplication.class, args);
    }
}

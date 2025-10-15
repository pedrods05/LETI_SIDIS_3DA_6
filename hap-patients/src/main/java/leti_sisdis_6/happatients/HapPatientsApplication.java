package leti_sisdis_6.happatients;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "leti_sisdis_6.happatients",
        "leti_sisdis_6.hapauth"
})
@EnableJpaRepositories(basePackages = "leti_sisdis_6.happatients.repository")

public class HapPatientsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HapPatientsApplication.class, args);
    }

}

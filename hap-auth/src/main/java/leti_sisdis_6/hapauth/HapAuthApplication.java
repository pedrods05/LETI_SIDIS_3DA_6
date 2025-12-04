package leti_sisdis_6.hapauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "leti_sisdis_6.hapauth"
})
@EnableJpaRepositories(basePackages = "leti_sisdis_6.hapauth.usermanagement")
public class HapAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(HapAuthApplication.class, args);
    }

}

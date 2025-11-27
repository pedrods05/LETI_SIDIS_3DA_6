package leti_sisdis_6.happhysicians;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {
        "leti_sisdis_6.happhysicians"
})
@EnableJpaRepositories(basePackages = {
        "leti_sisdis_6.happhysicians.repository"
})
@EnableMongoRepositories(basePackages = {
        "leti_sisdis_6.happhysicians.query"
})
@EnableRabbit
@EntityScan(basePackages = {
        "leti_sisdis_6.happhysicians.model"
})
public class HapPhysiciansApplication {

    public static void main(String[] args) {
        SpringApplication.run(HapPhysiciansApplication.class, args);
    }
}

package leti_sisdis_6.happatients.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "leti_sisdis_6.happatients.repository")
public class JpaConfig {
}

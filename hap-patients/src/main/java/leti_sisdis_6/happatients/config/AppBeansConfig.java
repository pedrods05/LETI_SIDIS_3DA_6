package leti_sisdis_6.happatients.config;

import leti_sisdis_6.happatients.http.ResilientRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppBeansConfig {

    @Bean
    public ResilientRestTemplate resilientRestTemplate(RestTemplate restTemplate) {
        return new ResilientRestTemplate(restTemplate);
    }
}

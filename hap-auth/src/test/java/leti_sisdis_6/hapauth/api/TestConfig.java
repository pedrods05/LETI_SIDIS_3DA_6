package leti_sisdis_6.hapauth.api;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public EntityManagerFactory entityManagerFactory() {
        return mock(EntityManagerFactory.class);
    }
}
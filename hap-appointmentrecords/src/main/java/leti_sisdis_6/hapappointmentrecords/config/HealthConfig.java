package leti_sisdis_6.hapappointmentrecords.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthConfig {

    @Bean
    public HealthIndicator amqpHealthIndicator(RabbitTemplate rabbitTemplate) {
        return () -> {
            try {
                // Verifica se o broker RabbitMQ está acessível
                rabbitTemplate.execute(channel -> channel.getConnection().getServerProperties());
                return Health.up().withDetail("amqp", "RabbitMQ is reachable").build();
            } catch (Exception e) {
                return Health.down().withDetail("amqp", "RabbitMQ is unreachable").withException(e).build();
            }
        };
    }
}
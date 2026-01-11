package leti_sisdis_6.hapauth.configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ResilienceAlertConfig {

    @Autowired
    private CircuitBreakerRegistry registry;

    @PostConstruct
    public void setupAlerts() {
        registry.getAllCircuitBreakers().forEach(cb -> {
            cb.getEventPublisher().onStateTransition(event ->
                    log.error("RESILIÊNCIA: CB '{}' mudou de {} para {}",
                            event.getCircuitBreakerName(),
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState())
            );
        });
    }
}
package leti_sisdis_6.happatients.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ResilienceAlertConfig {

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Bean
    public HealthIndicator circuitBreakerHealthIndicator() {
        return new HealthIndicator() {
            @Override
            public Health health() {
                if (circuitBreakerRegistry == null) {
                    return Health.up().withDetail("status", "Circuit Breaker Registry not available").build();
                }

                Health.Builder builder = Health.up();
                boolean anyOpen = false;

                // Check all circuit breakers
                circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
                    CircuitBreaker.State state = circuitBreaker.getState();
                    builder.withDetail(circuitBreaker.getName(), state.name());

                    if (state == CircuitBreaker.State.OPEN) {
                        log.warn("⚠️ [Alert] Circuit Breaker '{}' is OPEN - service may be unavailable",
                                circuitBreaker.getName());
                    }
                });

                // Check if any circuit breaker is open
                anyOpen = circuitBreakerRegistry.getAllCircuitBreakers().stream()
                        .anyMatch(cb -> cb.getState() == CircuitBreaker.State.OPEN);

                if (anyOpen) {
                    return builder.down()
                            .withDetail("alert", "One or more circuit breakers are OPEN")
                            .build();
                }

                return builder.build();
            }
        };
    }

    @Bean
    public String registerCircuitBreakerMetrics() {
        if (meterRegistry != null && circuitBreakerRegistry != null) {
            Gauge.builder("resilience4j.circuitbreaker.state",
                            circuitBreakerRegistry,
                            registry -> {
                                long openCount = registry.getAllCircuitBreakers().stream()
                                        .filter(cb -> cb.getState() == CircuitBreaker.State.OPEN)
                                        .count();
                                return openCount;
                            })
                    .description("Number of Circuit Breakers in OPEN state")
                    .tag("application", "hap-patients")
                    .register(meterRegistry);
            log.info("✅ Circuit Breaker metrics registered for alerting");
        }
        return "circuitBreakerMetricsRegistered";
    }

    @Bean
    public Counter amqpFailureRateCounter() {
        if (meterRegistry == null) {
            return null;
        }

        return Counter.builder("amqp.messages.failure.rate")
                .description("Rate of AMQP message failures for alerting")
                .tag("application", "hap-patients")
                .register(meterRegistry);
    }
}


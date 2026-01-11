package leti_sisdis_6.happhysicians.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {
    @Bean
    public Timer sagaStepDurationTimer(MeterRegistry registry) {
        return Timer.builder("saga.step.duration")
                .description("Time taken to execute saga step")
                .tag("application", "hap-physicians")
                .register(registry);
    }
    @Bean
    public Counter sagaCompensationCounter(MeterRegistry registry) {
        return Counter.builder("saga.compensation.count")
                .description("Number of compensations triggered")
                .tag("application", "hap-physicians")
                .register(registry);
    }
    @Bean
    public Counter amqpMessagesPublishedCounter(MeterRegistry registry) {
        return Counter.builder("amqp.messages.published")
                .description("Number of AMQP messages published")
                .tag("application", "hap-physicians")
                .register(registry);
    }
    @Bean
    public Counter amqpMessagesConsumedCounter(MeterRegistry registry) {
        return Counter.builder("amqp.messages.consumed")
                .description("Number of AMQP messages consumed")
                .tag("application", "hap-physicians")
                .register(registry);
    }
    @Bean
    public Counter amqpMessagesFailedCounter(MeterRegistry registry) {
        return Counter.builder("amqp.messages.failed")
                .description("Number of failed AMQP message processing attempts")
                .tag("application", "hap-physicians")
                .register(registry);
    }
}


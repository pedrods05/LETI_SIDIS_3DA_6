package leti_sisdis_6.happhysicians.services;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompensationService {

    private final MeterRegistry meterRegistry;

    @Bulkhead(name = "compensation", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<Void> executeCompensation(Runnable compensationLogic) {
        Counter compensationCounter = Counter.builder("saga.compensation.count")
                .description("Number of compensations triggered")
                .tag("application", "hap-physicians")
                .register(meterRegistry);

        return CompletableFuture.runAsync(() -> {
            try {
                log.info("🔄 [Compensation] Starting compensation operation with Bulkhead isolation");
                compensationCounter.increment();
                compensationLogic.run();
                log.info("✅ [Compensation] Compensation operation completed successfully");
            } catch (Exception e) {
                log.error("❌ [Compensation] Compensation operation failed: {}", e.getMessage(), e);
                throw e;
            }
        });
    }
}


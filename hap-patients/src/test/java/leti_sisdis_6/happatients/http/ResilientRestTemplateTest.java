package leti_sisdis_6.happatients.http;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResilientRestTemplateTest {

    @Test
    void peerCooldownPreventsImmediateRetry() {
        RestTemplate rt = new RestTemplate();
        ResilientRestTemplate resilient = new ResilientRestTemplate(rt);
        // First call will fail fast (invalid URL / refused port) and mark as failed
        assertThrows(Exception.class, () -> resilient.getForObjectWithFallback("http://localhost:1/bad", String.class));
        // Immediately after, it should return null (cooldown active)
        String res = resilient.getForObjectWithFallback("http://localhost:1/another", String.class);
        assertThat(res).isNull();
    }
}


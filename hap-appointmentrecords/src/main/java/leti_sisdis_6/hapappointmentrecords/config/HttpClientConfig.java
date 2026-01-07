package leti_sisdis_6.hapappointmentrecords.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.slf4j.MDC;

import java.time.Duration;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.CORRELATION_ID_HEADER;

@Configuration
@EnableRetry
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Solução para Spring Boot 3.4.0+: 
        // Configuramos o HttpClient nativo do Java e passamos para a factory
        RestTemplate restTemplate = builder
                .requestFactory(() -> {
                    HttpClient httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(3)) // Timeout de conexão
                            .build();

                    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
                    factory.setReadTimeout(Duration.ofSeconds(5)); // Timeout de leitura
                    return factory;
                })
                .build();

        // Configuração de Interceptores (Segurança e Tracing)
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());

        interceptors.add((request, body, execution) -> {
            // Propagação de Segurança (JWT)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getCredentials() instanceof String token) {
                if (!token.isBlank()) {
                    request.getHeaders().set("Authorization", "Bearer " + token);
                }
            }

            // Propagação de Tracing para ELK/Zipkin
            String correlationId = MDC.get(CORRELATION_ID_HEADER);
            if (correlationId != null && !correlationId.isBlank()) {
                request.getHeaders().set(CORRELATION_ID_HEADER, correlationId);
            }

            return execution.execute(request, body);
        });

        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}
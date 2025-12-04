package leti_sisdis_6.happatients.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableRetry
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();

        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
        interceptors.add((request, body, execution) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getCredentials() instanceof String) {
                String token = (String) authentication.getCredentials();
                if (token != null && !token.isBlank()) {
                    request.getHeaders().set("Authorization", "Bearer " + token);
                }
            }
            return execution.execute(request, body);
        });
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}



package leti_sisdis_6.hapauth.configuration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.retry.annotation.EnableRetry;
import java.time.Duration;
import java.net.http.HttpClient;

@Configuration
@EnableRetry
public class HttpClientConfig {

    // Renomeado para evitar conflito com o bean do SecurityConfig
    @Bean(name = "resilientRestTemplate")
    public RestTemplate resilientRestTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(() -> {
                    HttpClient httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(3))
                            .build();
                    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
                    factory.setReadTimeout(Duration.ofSeconds(5));
                    return factory;
                })
                .build();
    }
}
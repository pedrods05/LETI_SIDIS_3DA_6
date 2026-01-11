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
import org.springframework.beans.factory.annotation.Value;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.KeyManagerFactory;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.net.ssl.SSLParameters;

import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.CORRELATION_ID_HEADER;

@Configuration
@EnableRetry
public class HttpClientConfig {

    @Value("${hap.mtls.enabled:false}")
    private boolean mtlsEnabled;

    @Value("${hap.mtls.truststore.path:}")
    private String truststorePath;

    @Value("${hap.mtls.truststore.password:}")
    private String truststorePassword;

    @Value("${hap.mtls.keystore.path:}")
    private String keystorePath;

    @Value("${hap.mtls.keystore.password:}")
    private String keystorePassword;

    @Value("${hap.ssl.disable-hostname-verification:false}")
    private boolean disableHostnameVerification;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        if (disableHostnameVerification) {
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
        }
        RestTemplate restTemplate = builder
                .requestFactory(() -> {
                    HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(3)); // Timeout de conexão

                    if (mtlsEnabled) {
                        clientBuilder.sslContext(buildSslContext());
                    }

                    if (disableHostnameVerification) {
                        SSLParameters sslParams = new SSLParameters();
                        sslParams.setEndpointIdentificationAlgorithm(null);
                        clientBuilder.sslParameters(sslParams);
                    }

                    HttpClient httpClient = clientBuilder.build();

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

    private SSLContext buildSslContext() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

            if (truststorePath != null && !truststorePath.isBlank() && Files.exists(Path.of(truststorePath))) {
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                try (FileInputStream fis = new FileInputStream(truststorePath)) {
                    trustStore.load(fis, truststorePassword != null ? truststorePassword.toCharArray() : null);
                }
                tmf.init(trustStore);
            } else {
                tmf.init((KeyStore) null);
            }

            if (keystorePath != null && !keystorePath.isBlank() && Files.exists(Path.of(keystorePath))) {
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                try (FileInputStream fis = new FileInputStream(keystorePath)) {
                    keyStore.load(fis, keystorePassword != null ? keystorePassword.toCharArray() : null);
                }
                kmf.init(keyStore, keystorePassword != null ? keystorePassword.toCharArray() : null);
            } else {
                kmf.init(null, null);
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            return sslContext;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build SSLContext for mTLS", ex);
        }
    }
}
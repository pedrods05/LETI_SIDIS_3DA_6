package leti_sisdis_6.hapappointmentrecords.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;

import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.CORRELATION_ID_HEADER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HttpClientConfigTest {

    @Test
    @DisplayName("Interceptor deve adicionar Authorization e Correlation-Id")
    void restTemplate_InterceptorLogic() throws IOException {
        // Arrange
        HttpClientConfig config = new HttpClientConfig();
        RestTemplateBuilder builder = new RestTemplateBuilder();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getCredentials()).thenReturn("dummy-jwt-token");
        SecurityContextHolder.setContext(securityContext);

        MDC.put(CORRELATION_ID_HEADER, "test-correlation-id");

        RestTemplate restTemplate = config.restTemplate(builder);

        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);

        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        byte[] body = new byte[0];

        interceptor.intercept(request, body, execution);
        assertEquals("Bearer dummy-jwt-token", headers.getFirst("Authorization"));
        assertEquals("test-correlation-id", headers.getFirst(CORRELATION_ID_HEADER));
        verify(execution).execute(request, body);

        MDC.clear();
        SecurityContextHolder.clearContext();
    }
}
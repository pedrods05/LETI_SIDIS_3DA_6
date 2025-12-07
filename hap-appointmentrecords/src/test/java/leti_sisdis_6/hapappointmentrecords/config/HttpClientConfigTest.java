package leti_sisdis_6.hapappointmentrecords.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.CORRELATION_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HttpClientConfigTest {

    private HttpClientConfig config;
    private RestTemplateBuilder builder;

    @BeforeEach
    void setUp() {
        config = new HttpClientConfig();
        builder = new RestTemplateBuilder();
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    @DisplayName("Should create RestTemplate with configured timeouts")
    void shouldCreateRestTemplateWithTimeouts() {
        // When
        RestTemplate restTemplate = config.restTemplate(builder);

        // Then
        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate.getRequestFactory()).isNotNull();
    }

    @Test
    @DisplayName("Should add interceptors to RestTemplate")
    void shouldAddInterceptorsToRestTemplate() {
        // When
        RestTemplate restTemplate = config.restTemplate(builder);

        // Then
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        assertThat(interceptors).isNotEmpty();
        assertThat(interceptors).hasSize(1);
    }

    @Test
    @DisplayName("Interceptor should add Authorization header when token is present")
    void interceptorShouldAddAuthorizationHeader() throws IOException {
        // Given
        String token = "test-jwt-token";
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", token);
        SecurityContextHolder.getContext().setAuthentication(auth);

        RestTemplate restTemplate = config.restTemplate(builder);
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);

        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(mockRequest.getHeaders()).thenReturn(headers);

        byte[] body = new byte[0];
        ClientHttpRequestExecution mockExecution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockExecution.execute(any(), any())).thenReturn(mockResponse);

        // When
        interceptor.intercept(mockRequest, body, mockExecution);

        // Then
        assertThat(headers.get("Authorization")).containsExactly("Bearer " + token);
    }

    @Test
    @DisplayName("Interceptor should add Correlation ID header when present in MDC")
    void interceptorShouldAddCorrelationIdHeader() throws IOException {
        // Given
        String correlationId = "test-correlation-123";
        MDC.put(CORRELATION_ID_HEADER, correlationId);

        RestTemplate restTemplate = config.restTemplate(builder);
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);

        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(mockRequest.getHeaders()).thenReturn(headers);

        byte[] body = new byte[0];
        ClientHttpRequestExecution mockExecution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockExecution.execute(any(), any())).thenReturn(mockResponse);

        // When
        interceptor.intercept(mockRequest, body, mockExecution);

        // Then
        assertThat(headers.get(CORRELATION_ID_HEADER)).containsExactly(correlationId);
    }

    @Test
    @DisplayName("Interceptor should not add Authorization header when token is null")
    void interceptorShouldNotAddAuthorizationHeaderWhenTokenIsNull() throws IOException {
        // Given
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        RestTemplate restTemplate = config.restTemplate(builder);
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);

        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(mockRequest.getHeaders()).thenReturn(headers);

        byte[] body = new byte[0];
        ClientHttpRequestExecution mockExecution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockExecution.execute(any(), any())).thenReturn(mockResponse);

        // When
        interceptor.intercept(mockRequest, body, mockExecution);

        // Then
        assertThat(headers.get("Authorization")).isNull();
    }

    @Test
    @DisplayName("Interceptor should not add Authorization header when token is blank")
    void interceptorShouldNotAddAuthorizationHeaderWhenTokenIsBlank() throws IOException {
        // Given
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", "   ");
        SecurityContextHolder.getContext().setAuthentication(auth);

        RestTemplate restTemplate = config.restTemplate(builder);
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);

        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(mockRequest.getHeaders()).thenReturn(headers);

        byte[] body = new byte[0];
        ClientHttpRequestExecution mockExecution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockExecution.execute(any(), any())).thenReturn(mockResponse);

        // When
        interceptor.intercept(mockRequest, body, mockExecution);

        // Then
        assertThat(headers.get("Authorization")).isNull();
    }

    @Test
    @DisplayName("Interceptor should not add Correlation ID header when not in MDC")
    void interceptorShouldNotAddCorrelationIdHeaderWhenNotInMDC() throws IOException {
        // Given
        MDC.clear();

        RestTemplate restTemplate = config.restTemplate(builder);
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);

        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(mockRequest.getHeaders()).thenReturn(headers);

        byte[] body = new byte[0];
        ClientHttpRequestExecution mockExecution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockExecution.execute(any(), any())).thenReturn(mockResponse);

        // When
        interceptor.intercept(mockRequest, body, mockExecution);

        // Then
        assertThat(headers.get(CORRELATION_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Interceptor should not add Correlation ID header when blank in MDC")
    void interceptorShouldNotAddCorrelationIdHeaderWhenBlankInMDC() throws IOException {
        // Given
        MDC.put(CORRELATION_ID_HEADER, "   ");

        RestTemplate restTemplate = config.restTemplate(builder);
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);

        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(mockRequest.getHeaders()).thenReturn(headers);

        byte[] body = new byte[0];
        ClientHttpRequestExecution mockExecution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockExecution.execute(any(), any())).thenReturn(mockResponse);

        // When
        interceptor.intercept(mockRequest, body, mockExecution);

        // Then
        assertThat(headers.get(CORRELATION_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Interceptor should add both headers when both are available")
    void interceptorShouldAddBothHeaders() throws IOException {
        // Given
        String token = "test-jwt-token";
        String correlationId = "test-correlation-123";

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", token);
        SecurityContextHolder.getContext().setAuthentication(auth);
        MDC.put(CORRELATION_ID_HEADER, correlationId);

        RestTemplate restTemplate = config.restTemplate(builder);
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);

        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(mockRequest.getHeaders()).thenReturn(headers);

        byte[] body = new byte[0];
        ClientHttpRequestExecution mockExecution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockExecution.execute(any(), any())).thenReturn(mockResponse);

        // When
        interceptor.intercept(mockRequest, body, mockExecution);

        // Then
        assertThat(headers.get("Authorization")).containsExactly("Bearer " + token);
        assertThat(headers.get(CORRELATION_ID_HEADER)).containsExactly(correlationId);
    }

    @Test
    @DisplayName("Interceptor should execute the request chain")
    void interceptorShouldExecuteRequestChain() throws IOException {
        // Given
        RestTemplate restTemplate = config.restTemplate(builder);
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);

        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(mockRequest.getHeaders()).thenReturn(headers);

        byte[] body = new byte[0];
        ClientHttpRequestExecution mockExecution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockExecution.execute(any(), any())).thenReturn(mockResponse);

        // When
        ClientHttpResponse response = interceptor.intercept(mockRequest, body, mockExecution);

        // Then
        assertThat(response).isEqualTo(mockResponse);
        verify(mockExecution).execute(mockRequest, body);
    }

    @Test
    @DisplayName("Should handle authentication with non-String credentials gracefully")
    void shouldHandleNonStringCredentialsGracefully() throws IOException {
        // Given
        Object nonStringCredentials = new Object();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", nonStringCredentials);
        SecurityContextHolder.getContext().setAuthentication(auth);

        RestTemplate restTemplate = config.restTemplate(builder);
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);

        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(mockRequest.getHeaders()).thenReturn(headers);

        byte[] body = new byte[0];
        ClientHttpRequestExecution mockExecution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
        when(mockExecution.execute(any(), any())).thenReturn(mockResponse);

        // When
        interceptor.intercept(mockRequest, body, mockExecution);

        // Then
        assertThat(headers.get("Authorization")).isNull();
    }
}


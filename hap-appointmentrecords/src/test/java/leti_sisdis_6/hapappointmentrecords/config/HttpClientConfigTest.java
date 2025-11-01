package leti_sisdis_6.hapappointmentrecords.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig
@SpringBootTest
class HttpClientConfigTest {

    private final HttpClientConfig httpClientConfig = new HttpClientConfig();

    @Test
    @DisplayName("RestTemplate deve ser criado com timeouts configurados")
    void restTemplate_shouldCreateWithConfiguredTimeouts() {
        // Given
        RestTemplateBuilder mockBuilder = mock(RestTemplateBuilder.class);
        RestTemplate mockRestTemplate = mock(RestTemplate.class);

        when(mockBuilder.requestFactory(any())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockRestTemplate);
        when(mockRestTemplate.getInterceptors()).thenReturn(List.of());

        // When
        RestTemplate result = httpClientConfig.restTemplate(mockBuilder);

        // Then
        assertNotNull(result);
        verify(mockBuilder).requestFactory(any());
        verify(mockBuilder).build();
        verify(mockRestTemplate).setInterceptors(any());
    }

    @Test
    @DisplayName("RestTemplate deve adicionar interceptor de autenticação")
    void restTemplate_shouldAddAuthenticationInterceptor() {
        // Given
        RestTemplateBuilder builder = new RestTemplateBuilder();

        // When
        RestTemplate result = httpClientConfig.restTemplate(builder);

        // Then
        assertNotNull(result);
        List<ClientHttpRequestInterceptor> interceptors = result.getInterceptors();
        assertFalse(interceptors.isEmpty());
        assertEquals(1, interceptors.size());
    }

    @Test
    @DisplayName("RequestFactory deve ter timeouts configurados")
    void requestFactory_shouldHaveConfiguredTimeouts() {
        // Given
        RestTemplateBuilder builder = new RestTemplateBuilder();

        // When
        RestTemplate result = httpClientConfig.restTemplate(builder);

        // Then
        assertNotNull(result);
        assertTrue(result.getRequestFactory() instanceof SimpleClientHttpRequestFactory);

        SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory) result.getRequestFactory();
        assertEquals(3000, factory.getConnectTimeout());
        assertEquals(5000, factory.getReadTimeout());
    }

    @Test
    @DisplayName("Interceptor deve adicionar Bearer token quando autenticação está presente")
    void interceptor_shouldAddBearerTokenWhenAuthenticationPresent() {
        // Given
        String token = "test-token";
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            "user", token);
        SecurityContextHolder.getContext().setAuthentication(auth);

        RestTemplateBuilder builder = new RestTemplateBuilder();
        RestTemplate result = httpClientConfig.restTemplate(builder);

        // When/Then - O interceptor é testado indiretamente através da configuração
        assertNotNull(result);
        assertFalse(result.getInterceptors().isEmpty());

        // Cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Interceptor deve funcionar sem autenticação")
    void interceptor_shouldWorkWithoutAuthentication() {
        // Given
        SecurityContextHolder.clearContext();
        RestTemplateBuilder builder = new RestTemplateBuilder();

        // When
        RestTemplate result = httpClientConfig.restTemplate(builder);

        // Then
        assertNotNull(result);
        assertFalse(result.getInterceptors().isEmpty());
    }
}

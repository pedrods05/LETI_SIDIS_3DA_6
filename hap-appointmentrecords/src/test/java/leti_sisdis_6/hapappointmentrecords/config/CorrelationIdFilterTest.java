package leti_sisdis_6.hapappointmentrecords.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.CORRELATION_ID_HEADER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Deve usar o Correlation ID existente no header do request")
    void doFilterInternal_WithExistingHeader() throws ServletException, IOException {
        String existingId = "123-abc";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CORRELATION_ID_HEADER, existingId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        doAnswer(invocation -> {
            assertEquals(existingId, MDC.get(CORRELATION_ID_HEADER));
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(existingId, response.getHeader(CORRELATION_ID_HEADER));
        verify(filterChain).doFilter(request, response);
        assertNull(MDC.get(CORRELATION_ID_HEADER), "MDC deve ser limpo após o filtro");
    }

    @Test
    @DisplayName("Deve gerar novo UUID se o header não existir")
    void doFilterInternal_GenerateNewId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        filter.doFilterInternal(request, response, filterChain);
        String responseHeader = response.getHeader(CORRELATION_ID_HEADER);
        assertNotNull(responseHeader);
        assertFalse(responseHeader.isBlank());
        verify(filterChain).doFilter(request, response);
    }
}
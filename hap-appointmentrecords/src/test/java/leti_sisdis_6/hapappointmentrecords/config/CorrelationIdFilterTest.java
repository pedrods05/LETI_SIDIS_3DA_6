package leti_sisdis_6.hapappointmentrecords.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.CORRELATION_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        MDC.clear();
    }

    @Test
    @DisplayName("Should generate correlation ID when not present in request")
    void shouldGenerateCorrelationIdWhenNotPresent() throws ServletException, IOException {
        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        String responseHeader = response.getHeader(CORRELATION_ID_HEADER);
        assertThat(responseHeader).isNotNull();
        assertThat(responseHeader).isNotBlank();
        assertThat(responseHeader).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"); // UUID format
    }

    @Test
    @DisplayName("Should use existing correlation ID from request header")
    void shouldUseExistingCorrelationIdFromHeader() throws ServletException, IOException {
        // Given
        String existingCorrelationId = "test-correlation-id-123";
        request.addHeader(CORRELATION_ID_HEADER, existingCorrelationId);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        String responseHeader = response.getHeader(CORRELATION_ID_HEADER);
        assertThat(responseHeader).isEqualTo(existingCorrelationId);
    }

    @Test
    @DisplayName("Should add correlation ID to MDC during request")
    void shouldAddCorrelationIdToMDCDuringRequest() throws ServletException, IOException {
        // Given
        String[] capturedMdcValue = new String[1];
        MockFilterChain customFilterChain = new MockFilterChain(null, null) {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
                capturedMdcValue[0] = MDC.get(CORRELATION_ID_HEADER);
            }
        };

        // When
        filter.doFilterInternal(request, response, customFilterChain);

        // Then
        assertThat(capturedMdcValue[0]).isNotNull();
        assertThat(capturedMdcValue[0]).isNotBlank();
    }

    @Test
    @DisplayName("Should remove correlation ID from MDC after request")
    void shouldRemoveCorrelationIdFromMDCAfterRequest() throws ServletException, IOException {
        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        String mdcValue = MDC.get(CORRELATION_ID_HEADER);
        assertThat(mdcValue).isNull();
    }

    @Test
    @DisplayName("Should remove correlation ID from MDC even when exception occurs")
    void shouldRemoveCorrelationIdFromMDCEvenWhenExceptionOccurs() {
        // Given
        MockFilterChain errorFilterChain = new MockFilterChain(null, null) {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
                    throws IOException, ServletException {
                throw new ServletException("Test exception");
            }
        };

        // When
        try {
            filter.doFilterInternal(request, response, errorFilterChain);
        } catch (Exception e) {
            // Expected
        }

        // Then
        String mdcValue = MDC.get(CORRELATION_ID_HEADER);
        assertThat(mdcValue).isNull();
    }

    @Test
    @DisplayName("Should generate new correlation ID when header is blank")
    void shouldGenerateNewCorrelationIdWhenHeaderIsBlank() throws ServletException, IOException {
        // Given
        request.addHeader(CORRELATION_ID_HEADER, "   ");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        String responseHeader = response.getHeader(CORRELATION_ID_HEADER);
        assertThat(responseHeader).isNotNull();
        assertThat(responseHeader).isNotBlank();
        assertThat(responseHeader).isNotEqualTo("   ");
    }

    @Test
    @DisplayName("Should set correlation ID as response header")
    void shouldSetCorrelationIdAsResponseHeader() throws ServletException, IOException {
        // Given
        String correlationId = "my-custom-correlation-id";
        request.addHeader(CORRELATION_ID_HEADER, correlationId);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(response.getHeader(CORRELATION_ID_HEADER)).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should maintain correlation ID consistency between MDC and response")
    void shouldMaintainCorrelationIdConsistency() throws ServletException, IOException {
        // Given
        String[] mdcValueDuringRequest = new String[1];
        MockFilterChain customFilterChain = new MockFilterChain(null, null) {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
                mdcValueDuringRequest[0] = MDC.get(CORRELATION_ID_HEADER);
            }
        };

        // When
        filter.doFilterInternal(request, response, customFilterChain);

        // Then
        String responseHeader = response.getHeader(CORRELATION_ID_HEADER);
        assertThat(mdcValueDuringRequest[0]).isEqualTo(responseHeader);
    }
}


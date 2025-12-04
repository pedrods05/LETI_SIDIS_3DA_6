package leti_sisdis_6.happhysicians.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ValidationException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private WebRequest webRequest;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void testHandleConflict_ConflictException() {
        // Arrange
        ConflictException ex = new ConflictException("Resource conflict");

        // Act
        ResponseEntity<Object> response = globalExceptionHandler.handleConflict(request, ex);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void testHandleConstraintViolation() {
        // Arrange
        SQLException sqlException = new SQLException("Constraint violation");
        ConstraintViolationException ex = new ConstraintViolationException("Message", sqlException, "constraint_name");

        // Act
        ResponseEntity<Object> response = globalExceptionHandler.handleConstraintViolation(request, ex);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void testHandleDataIntegrityViolation() {
        // Arrange
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Data integrity violation");

        // Act
        ResponseEntity<Object> response = globalExceptionHandler.handleDataIntegrityViolation(request, ex);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void testHandleIllegalArgument() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        // Act
        ResponseEntity<Object> response = globalExceptionHandler.handleIllegalArgument(request, ex);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHandleNotFoundException() {
        // Arrange
        NotFoundException ex = new NotFoundException("Resource not found");

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiCallError<String>> response = 
                globalExceptionHandler.handleNotFoundException(request, ex);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testHandleValidationException() {
        // Arrange
        ValidationException ex = new ValidationException("Validation failed");

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiCallError<String>> response = 
                globalExceptionHandler.handleValidationException(request, ex);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testHandleMethodArgumentTypeMismatchException() {
        // Arrange
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "invalidValue", Integer.class, "paramName", null, new Throwable());

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiCallError<Map.Entry<String, String>>> response = 
                globalExceptionHandler.handleMethodArgumentTypeMismatchException(request, ex);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testHandleMethodArgumentNotValid() {
        // Arrange
        List<FieldError> fieldErrors = new ArrayList<>();
        FieldError fieldError = new FieldError("object", "field", "rejectedValue", 
                false, null, null, "error message");
        fieldErrors.add(fieldError);

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        HttpHeaders headers = new HttpHeaders();

        // Act
        ResponseEntity<Object> response = globalExceptionHandler.handleMethodArgumentNotValid(
                ex, headers, HttpStatus.BAD_REQUEST, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testHandleAccessDeniedException() {
        // Arrange
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiCallError<String>> response = 
                globalExceptionHandler.handleAccessDeniedException(request, ex);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testHandleUserNotFoundException() {
        // Arrange
        UserNotFoundException ex = new UserNotFoundException("User not found");

        // Act
        ResponseEntity<GlobalExceptionHandler.ApiCallError<String>> response = 
                globalExceptionHandler.handleUserNotFoundException(request, ex);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testApiCallError_Constructor() {
        // Arrange
        List<String> details = List.of("Detail 1", "Detail 2");

        // Act
        GlobalExceptionHandler.ApiCallError<String> error = 
                new GlobalExceptionHandler.ApiCallError<>("Test message", details);

        // Assert
        assertNotNull(error);
        assertEquals("Test message", error.getMessage());
        assertEquals(2, error.getDetails().size());
    }
}


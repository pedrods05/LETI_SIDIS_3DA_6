package leti_sisdis_6.happhysicians.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PatientNotFoundException extends RuntimeException {
    public PatientNotFoundException(String message) {
        super(message);
    }

    public PatientNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

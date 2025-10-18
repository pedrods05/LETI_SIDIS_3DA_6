package leti_sisdis_6.happhysicians.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AppointmentRecordNotFoundException extends RuntimeException {
    public AppointmentRecordNotFoundException(String message) {
        super(message);
    }

    public AppointmentRecordNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

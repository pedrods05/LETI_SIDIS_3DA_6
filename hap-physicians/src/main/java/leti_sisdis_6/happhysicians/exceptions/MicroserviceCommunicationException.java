package leti_sisdis_6.happhysicians.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class MicroserviceCommunicationException extends RuntimeException {
    private final String serviceName;
    private final String operation;

    public MicroserviceCommunicationException(String serviceName, String operation, String message, Throwable cause) {
        super(String.format("Error communicating with %s service during %s operation: %s", serviceName, operation, message), cause);
        this.serviceName = serviceName;
        this.operation = operation;
    }

    public MicroserviceCommunicationException(String serviceName, String operation, String message) {
        super(String.format("Error communicating with %s service during %s operation: %s", serviceName, operation, message));
        this.serviceName = serviceName;
        this.operation = operation;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getOperation() {
        return operation;
    }
}

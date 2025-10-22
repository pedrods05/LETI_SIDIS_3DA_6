package leti_sisdis_6.hapappointmentrecords.exceptions;

public class MicroserviceCommunicationException extends RuntimeException {
    private final String serviceName;
    private final String operation;
    private final String details;

    public MicroserviceCommunicationException(String serviceName, String operation, String details, Throwable cause) {
        super(String.format("Communication error with %s service during %s: %s", serviceName, operation, details), cause);
        this.serviceName = serviceName;
        this.operation = operation;
        this.details = details;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getOperation() {
        return operation;
    }

    public String getDetails() {
        return details;
    }
}


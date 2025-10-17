package leti_sisdis_6.hapappointmentrecords.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String email) {
        super(String.format("User account not found. Please register as a patient first. Email: %s", email));
    }
} 
package leti_sisdis_6.happhysicians.model;

public enum AppointmentStatus {
    SCHEDULED("SCHEDULED"),
    CANCELED("CANCELED"),
    COMPLETED("COMPLETED");

    private final String value;

    AppointmentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AppointmentStatus fromString(String text) {
        for (AppointmentStatus status : AppointmentStatus.values()) {
            if (status.value.equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid appointment status: " + text);
    }
}

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
        if (text == null) throw new IllegalArgumentException("Invalid appointment status: null");
        String normalized = text.trim().toUpperCase();
        if (normalized.equals("CANCELLED")) {
            // tolerate double-L from other services
            return CANCELED;
        }
        for (AppointmentStatus status : AppointmentStatus.values()) {
            if (status.value.equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid appointment status: " + text);
    }
}

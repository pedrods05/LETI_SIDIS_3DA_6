package com.pcm.psoft.pcmclinic_api.appointment.model;

public enum ConsultationType {
    FIRST_TIME("First-time"),
    FOLLOW_UP("Follow-up");

    private final String value;

    ConsultationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ConsultationType fromString(String text) {
        for (ConsultationType type : ConsultationType.values()) {
            if (type.value.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid consultation type: " + text);
    }
}
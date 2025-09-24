package com.pcm.psoft.pcmclinic_api.appointment.util;

import org.springframework.stereotype.Component;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class AppointmentTimeValidator {
    private static final int SLOT_DURATION_MINUTES = 20;

    public static boolean isValidWorkingHours(LocalDateTime dateTime) {
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        LocalTime time = dateTime.toLocalTime();

        if (dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        if (dayOfWeek == DayOfWeek.SATURDAY) {
            return time.isAfter(LocalTime.of(8, 59)) && time.isBefore(LocalTime.of(13, 1));
        }

        return (time.isAfter(LocalTime.of(8, 59)) && time.isBefore(LocalTime.of(13, 1))) ||
               (time.isAfter(LocalTime.of(13, 59)) && time.isBefore(LocalTime.of(20, 1)));
    }

    public static boolean isValidTimeSlot(LocalDateTime dateTime) {
        int minutes = dateTime.getMinute();
        return minutes % SLOT_DURATION_MINUTES == 0;
    }

    public void validateAppointmentTime(LocalDateTime dateTime) {
        // Check if it's a valid day (not Sunday)
        if (dateTime.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Appointments are not available on Sundays");
        }

        // Check if it's Saturday (only morning)
        if (dateTime.getDayOfWeek() == DayOfWeek.SATURDAY) {
            LocalTime time = dateTime.toLocalTime();
            if (time.isBefore(LocalTime.of(9, 0)) || time.isAfter(LocalTime.of(13, 0))) {
                throw new IllegalArgumentException("On Saturdays, appointments are only available from 9:00 to 13:00");
            }
        }

        // Check if it's a weekday (morning or afternoon)
        if (dateTime.getDayOfWeek() != DayOfWeek.SATURDAY) {
            LocalTime time = dateTime.toLocalTime();
            boolean isMorning = time.isAfter(LocalTime.of(8, 59)) && time.isBefore(LocalTime.of(13, 1));
            boolean isAfternoon = time.isAfter(LocalTime.of(13, 59)) && time.isBefore(LocalTime.of(20, 1));

            if (!isMorning && !isAfternoon) {
                throw new IllegalArgumentException("Appointments are only available from 9:00 to 13:00 and from 14:00 to 20:00 on weekdays");
            }
        }

        // Check if it's a valid 20-minute slot
        int minutes = dateTime.getMinute();
        if (minutes % 20 != 0) {
            throw new IllegalArgumentException("Appointments must be scheduled in 20-minute slots");
        }
    }
}

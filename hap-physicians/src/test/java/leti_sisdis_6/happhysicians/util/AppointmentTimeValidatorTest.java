package leti_sisdis_6.happhysicians.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentTimeValidatorTest {

    private AppointmentTimeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AppointmentTimeValidator();
    }

    @Test
    void testIsValidWorkingHours_WeekdayMorning() {
        // Arrange - Monday 10:00
        LocalDateTime mondayMorning = LocalDateTime.now()
                .with(DayOfWeek.MONDAY)
                .with(LocalTime.of(10, 0));

        // Act
        boolean result = AppointmentTimeValidator.isValidWorkingHours(mondayMorning);

        // Assert
        assertTrue(result, "Weekday morning should be valid");
    }

    @Test
    void testIsValidWorkingHours_WeekdayAfternoon() {
        // Arrange - Monday 15:00
        LocalDateTime mondayAfternoon = LocalDateTime.now()
                .with(DayOfWeek.MONDAY)
                .with(LocalTime.of(15, 0));

        // Act
        boolean result = AppointmentTimeValidator.isValidWorkingHours(mondayAfternoon);

        // Assert
        assertTrue(result, "Weekday afternoon should be valid");
    }

    @Test
    void testIsValidWorkingHours_SaturdayMorning() {
        // Arrange - Saturday 10:00
        LocalDateTime saturdayMorning = LocalDateTime.now()
                .with(DayOfWeek.SATURDAY)
                .with(LocalTime.of(10, 0));

        // Act
        boolean result = AppointmentTimeValidator.isValidWorkingHours(saturdayMorning);

        // Assert
        assertTrue(result, "Saturday morning should be valid");
    }

    @Test
    void testIsValidWorkingHours_SaturdayAfternoon() {
        // Arrange - Saturday 15:00
        LocalDateTime saturdayAfternoon = LocalDateTime.now()
                .with(DayOfWeek.SATURDAY)
                .with(LocalTime.of(15, 0));

        // Act
        boolean result = AppointmentTimeValidator.isValidWorkingHours(saturdayAfternoon);

        // Assert
        assertFalse(result, "Saturday afternoon should not be valid");
    }

    @Test
    void testIsValidWorkingHours_Sunday() {
        // Arrange - Sunday 10:00
        LocalDateTime sunday = LocalDateTime.now()
                .with(DayOfWeek.SUNDAY)
                .with(LocalTime.of(10, 0));

        // Act
        boolean result = AppointmentTimeValidator.isValidWorkingHours(sunday);

        // Assert
        assertFalse(result, "Sunday should not be valid");
    }

    @Test
    void testIsValidWorkingHours_BeforeWorkingHours() {
        // Arrange - Monday 8:00
        LocalDateTime tooEarly = LocalDateTime.now()
                .with(DayOfWeek.MONDAY)
                .with(LocalTime.of(8, 0));

        // Act
        boolean result = AppointmentTimeValidator.isValidWorkingHours(tooEarly);

        // Assert
        assertFalse(result, "Before 9:00 should not be valid");
    }

    @Test
    void testIsValidWorkingHours_AfterWorkingHours() {
        // Arrange - Monday 21:00
        LocalDateTime tooLate = LocalDateTime.now()
                .with(DayOfWeek.MONDAY)
                .with(LocalTime.of(21, 0));

        // Act
        boolean result = AppointmentTimeValidator.isValidWorkingHours(tooLate);

        // Assert
        assertFalse(result, "After 20:00 should not be valid");
    }

    @Test
    void testIsValidTimeSlot_Valid20MinuteSlot() {
        // Arrange - Time at :00
        LocalDateTime validSlot1 = LocalDateTime.now().withMinute(0);
        
        // Arrange - Time at :20
        LocalDateTime validSlot2 = LocalDateTime.now().withMinute(20);
        
        // Arrange - Time at :40
        LocalDateTime validSlot3 = LocalDateTime.now().withMinute(40);

        // Act & Assert
        assertTrue(AppointmentTimeValidator.isValidTimeSlot(validSlot1), "00 minutes should be valid");
        assertTrue(AppointmentTimeValidator.isValidTimeSlot(validSlot2), "20 minutes should be valid");
        assertTrue(AppointmentTimeValidator.isValidTimeSlot(validSlot3), "40 minutes should be valid");
    }

    @Test
    void testIsValidTimeSlot_InvalidTimeSlot() {
        // Arrange - Time at :15
        LocalDateTime invalidSlot1 = LocalDateTime.now().withMinute(15);
        
        // Arrange - Time at :30
        LocalDateTime invalidSlot2 = LocalDateTime.now().withMinute(30);
        
        // Arrange - Time at :45
        LocalDateTime invalidSlot3 = LocalDateTime.now().withMinute(45);

        // Act & Assert
        assertFalse(AppointmentTimeValidator.isValidTimeSlot(invalidSlot1), "15 minutes should not be valid");
        assertFalse(AppointmentTimeValidator.isValidTimeSlot(invalidSlot2), "30 minutes should not be valid");
        assertFalse(AppointmentTimeValidator.isValidTimeSlot(invalidSlot3), "45 minutes should not be valid");
    }

    @Test
    void testValidateAppointmentTime_Sunday_ThrowsException() {
        // Arrange
        LocalDateTime sunday = LocalDateTime.now()
                .with(DayOfWeek.SUNDAY)
                .with(LocalTime.of(10, 0));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validateAppointmentTime(sunday);
        });
        assertTrue(exception.getMessage().contains("Sunday"));
    }

    @Test
    void testValidateAppointmentTime_SaturdayAfternoon_ThrowsException() {
        // Arrange
        LocalDateTime saturdayAfternoon = LocalDateTime.now()
                .with(DayOfWeek.SATURDAY)
                .with(LocalTime.of(15, 0));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validateAppointmentTime(saturdayAfternoon);
        });
        assertTrue(exception.getMessage().contains("Saturday"));
    }

    @Test
    void testValidateAppointmentTime_WeekdayOutsideHours_ThrowsException() {
        // Arrange - Monday 8:00
        LocalDateTime tooEarly = LocalDateTime.now()
                .with(DayOfWeek.MONDAY)
                .with(LocalTime.of(8, 0));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validateAppointmentTime(tooEarly);
        });
        assertTrue(exception.getMessage().contains("9:00") || exception.getMessage().contains("14:00"));
    }

    @Test
    void testValidateAppointmentTime_InvalidTimeSlot_ThrowsException() {
        // Arrange - Monday 10:15 (not a 20-minute slot)
        LocalDateTime invalidSlot = LocalDateTime.now()
                .with(DayOfWeek.MONDAY)
                .with(LocalTime.of(10, 15));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validateAppointmentTime(invalidSlot);
        });
        assertTrue(exception.getMessage().contains("20-minute"));
    }

    @Test
    void testValidateAppointmentTime_ValidWeekdayMorning_Success() {
        // Arrange - Monday 10:00
        LocalDateTime validTime = LocalDateTime.now()
                .with(DayOfWeek.MONDAY)
                .with(LocalTime.of(10, 0));

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> {
            validator.validateAppointmentTime(validTime);
        });
    }

    @Test
    void testValidateAppointmentTime_ValidWeekdayAfternoon_Success() {
        // Arrange - Monday 15:00
        LocalDateTime validTime = LocalDateTime.now()
                .with(DayOfWeek.MONDAY)
                .with(LocalTime.of(15, 0));

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> {
            validator.validateAppointmentTime(validTime);
        });
    }

    @Test
    void testValidateAppointmentTime_ValidSaturdayMorning_Success() {
        // Arrange - Saturday 10:00
        LocalDateTime validTime = LocalDateTime.now()
                .with(DayOfWeek.SATURDAY)
                .with(LocalTime.of(10, 0));

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> {
            validator.validateAppointmentTime(validTime);
        });
    }
}


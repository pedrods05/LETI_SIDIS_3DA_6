package leti_sisdis_6.happhysicians.util;

import leti_sisdis_6.happhysicians.dto.response.AppointmentSlotDto;
import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.model.Specialty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SlotCalculatorTest {

    private SlotCalculator slotCalculator;
    private Physician testPhysician;
    private Department testDepartment;
    private Specialty testSpecialty;

    @BeforeEach
    void setUp() {
        slotCalculator = new SlotCalculator();
        
        testDepartment = Department.builder()
                .departmentId("DEPT01")
                .code("CARD")
                .name("Cardiology")
                .build();

        testSpecialty = Specialty.builder()
                .specialtyId("SPEC01")
                .name("Cardiologist")
                .build();

        testPhysician = Physician.builder()
                .physicianId("PHY01")
                .fullName("Dr. John Doe")
                .licenseNumber("LIC123")
                .username("john.doe@hospital.com")
                .workingHourStart(LocalTime.of(9, 0))
                .workingHourEnd(LocalTime.of(17, 0))
                .department(testDepartment)
                .specialty(testSpecialty)
                .build();
    }

    @Test
    void testGenerateAvailableSlots_NoAppointments() {
        // Arrange
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(7);
        List<Appointment> appointments = Collections.emptyList();

        // Act
        List<AppointmentSlotDto> slots = slotCalculator.generateAvailableSlots(
                testPhysician, appointments, startDate, endDate);

        // Assert
        assertNotNull(slots);
        assertFalse(slots.isEmpty());
        // Should have slots from 9:00 to 17:00 with 5-minute intervals
        // That's 8 hours = 480 minutes / 5 = 96 slots per day (minus overlaps)
    }

    @Test
    void testGenerateAvailableSlots_WithExistingAppointments() {
        // Arrange
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(1);
        
        // Create an appointment at 10:00
        LocalDateTime appointmentTime = startDate.atTime(10, 0);
        Appointment existingAppointment = Appointment.builder()
                .appointmentId("APT01")
                .patientId("PAT01")
                .physician(testPhysician)
                .dateTime(appointmentTime)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        List<Appointment> appointments = Collections.singletonList(existingAppointment);

        // Act
        List<AppointmentSlotDto> slots = slotCalculator.generateAvailableSlots(
                testPhysician, appointments, startDate, endDate);

        // Assert
        assertNotNull(slots);
        // The 10:00 slot should not be available
        boolean hasConflictingSlot = slots.stream()
                .anyMatch(slot -> slot.getDate().equals(startDate.toString()) 
                        && slot.getStartTime().equals("10:00"));
        assertFalse(hasConflictingSlot, "Should not have slot at 10:00");
    }

    @Test
    void testGenerateAvailableSlots_LimitsTo3Months() {
        // Arrange
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(4); // 4 months in the future
        List<Appointment> appointments = Collections.emptyList();

        // Act
        List<AppointmentSlotDto> slots = slotCalculator.generateAvailableSlots(
                testPhysician, appointments, startDate, endDate);

        // Assert
        assertNotNull(slots);
        // All slots should be within 3 months
        LocalDate maxDate = LocalDate.now().plusMonths(3);
        boolean allWithinLimit = slots.stream()
                .allMatch(slot -> LocalDate.parse(slot.getDate()).isBefore(maxDate.plusDays(1)));
        assertTrue(allWithinLimit, "All slots should be within 3 months");
    }

    @Test
    void testGenerateAvailableSlots_ExcludesPastSlotsForToday() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(1);
        List<Appointment> appointments = Collections.emptyList();
        
        // Set working hours to include current time
        LocalTime currentTime = LocalTime.now();
        if (currentTime.isAfter(testPhysician.getWorkingHourStart()) 
                && currentTime.isBefore(testPhysician.getWorkingHourEnd())) {
            
            // Act
            List<AppointmentSlotDto> slots = slotCalculator.generateAvailableSlots(
                    testPhysician, appointments, today, endDate);

            // Assert
            assertNotNull(slots);
            // For today, slots before current time should not be included
            boolean hasPastSlot = slots.stream()
                    .anyMatch(slot -> slot.getDate().equals(today.toString()) 
                            && LocalTime.parse(slot.getStartTime()).isBefore(currentTime));
            assertFalse(hasPastSlot, "Should not have past slots for today");
        }
    }

    @Test
    void testGenerateAvailableSlots_MultipleAppointments() {
        // Arrange
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(1);
        
        // Create multiple appointments
        List<Appointment> appointments = new ArrayList<>();
        appointments.add(Appointment.builder()
                .appointmentId("APT01")
                .patientId("PAT01")
                .physician(testPhysician)
                .dateTime(startDate.atTime(10, 0))
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .build());
        appointments.add(Appointment.builder()
                .appointmentId("APT02")
                .patientId("PAT02")
                .physician(testPhysician)
                .dateTime(startDate.atTime(14, 0))
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .build());

        // Act
        List<AppointmentSlotDto> slots = slotCalculator.generateAvailableSlots(
                testPhysician, appointments, startDate, endDate);

        // Assert
        assertNotNull(slots);
        // Both 10:00 and 14:00 slots should be excluded
        boolean has10Slot = slots.stream()
                .anyMatch(slot -> slot.getDate().equals(startDate.toString()) 
                        && slot.getStartTime().equals("10:00"));
        boolean has14Slot = slots.stream()
                .anyMatch(slot -> slot.getDate().equals(startDate.toString()) 
                        && slot.getStartTime().equals("14:00"));
        assertFalse(has10Slot, "Should not have slot at 10:00");
        assertFalse(has14Slot, "Should not have slot at 14:00");
    }

    @Test
    void testGenerateAvailableSlots_DifferentDays() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate endDate = tomorrow;
        
        // Appointment only on tomorrow
        Appointment appointment = Appointment.builder()
                .appointmentId("APT01")
                .patientId("PAT01")
                .physician(testPhysician)
                .dateTime(tomorrow.atTime(10, 0))
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        List<Appointment> appointments = Collections.singletonList(appointment);

        // Act
        List<AppointmentSlotDto> slots = slotCalculator.generateAvailableSlots(
                testPhysician, appointments, today, endDate);

        // Assert
        assertNotNull(slots);
        // Today should have available slots at 10:00
        boolean todayHas10Slot = slots.stream()
                .anyMatch(slot -> slot.getDate().equals(today.toString()) 
                        && slot.getStartTime().equals("10:00"));
        // Tomorrow should not have slot at 10:00
        boolean tomorrowHas10Slot = slots.stream()
                .anyMatch(slot -> slot.getDate().equals(tomorrow.toString()) 
                        && slot.getStartTime().equals("10:00"));
        
        // Note: Today might not have 10:00 slot if it's already past 10:00
        LocalTime currentTime = LocalTime.now();
        if (currentTime.isBefore(LocalTime.of(10, 0))) {
            assertTrue(todayHas10Slot, "Today should have 10:00 slot");
        }
        assertFalse(tomorrowHas10Slot, "Tomorrow should not have 10:00 slot");
    }
}


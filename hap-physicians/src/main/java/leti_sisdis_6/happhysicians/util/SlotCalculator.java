package leti_sisdis_6.happhysicians.util;

import leti_sisdis_6.happhysicians.dto.response.AppointmentSlotDto;
import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.Physician;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Component
public class SlotCalculator {
    private static final int SLOT_DURATION_MINUTES = 20;
    private static final int SLOT_INTERVAL_MINUTES = 5;

    public List<AppointmentSlotDto> generateAvailableSlots(Physician physician, List<Appointment> appointments, LocalDate startDate, LocalDate endDate) {
        List<AppointmentSlotDto> slots = new ArrayList<>();
        Map<LocalDate, List<Appointment>> apptByDate = new HashMap<>();
        for (Appointment appt : appointments) {
            LocalDate date = appt.getDateTime().toLocalDate();
            apptByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(appt);
        }
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<Appointment> dayAppointments = apptByDate.getOrDefault(date, Collections.emptyList());
            LocalTime periodStart = physician.getWorkingHourStart();
            LocalTime periodEnd = physician.getWorkingHourEnd();
            LocalTime slotStart = periodStart;
            while (!slotStart.plusMinutes(SLOT_DURATION_MINUTES).isAfter(periodEnd)) {
                final LocalTime currentSlotStart = slotStart;
                final LocalTime currentSlotEnd = slotStart.plusMinutes(SLOT_DURATION_MINUTES);
                boolean overlaps = dayAppointments.stream().anyMatch(appt -> {
                    LocalTime apptStart = appt.getDateTime().toLocalTime();
                    LocalTime apptEnd = apptStart.plusMinutes(SLOT_DURATION_MINUTES);
                    return !(currentSlotEnd.isBefore(apptStart) || currentSlotStart.isAfter(apptEnd));
                });
                if (!overlaps) {
                    // Para o dia de hoje, só slots futuros
                    if (!date.isEqual(LocalDate.now()) || currentSlotStart.isAfter(LocalTime.now())) {
                        slots.add(new AppointmentSlotDto(
                                date.toString(),
                                currentSlotStart.toString(),
                                currentSlotEnd.toString()
                        ));
                    }
                }
                slotStart = slotStart.plusMinutes(SLOT_INTERVAL_MINUTES);
            }
        }
        // Limitar a 3 meses no futuro
        LocalDate maxDate = LocalDate.now().plusMonths(3);
        return slots.stream().filter(s -> LocalDate.parse(s.getDate()).isBefore(maxDate.plusDays(1))).toList();
    }
}

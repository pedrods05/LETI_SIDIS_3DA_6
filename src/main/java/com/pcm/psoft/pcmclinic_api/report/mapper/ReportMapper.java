package com.pcm.psoft.pcmclinic_api.report.mapper;

import com.pcm.psoft.pcmclinic_api.appointment.model.Appointment;
import com.pcm.psoft.pcmclinic_api.report.dto.AgeGroupStatsDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ReportMapper {
    public List<AgeGroupStatsDTO> toAgeGroupStatsDTO(List<Appointment> appointments) {
        Map<String, List<Appointment>> groupMap = new HashMap<>();
        groupMap.put("0-17", new ArrayList<>());
        groupMap.put("18-35", new ArrayList<>());
        groupMap.put("36-59", new ArrayList<>());
        groupMap.put("60+", new ArrayList<>());

        for (Appointment appt : appointments) {
            LocalDate birthDate = appt.getPatient().getBirthDate();
            if (birthDate == null) continue;
            int age = Period.between(birthDate, LocalDate.now()).getYears();
            String group;
            if (age <= 17) group = "0-17";
            else if (age <= 35) group = "18-35";
            else if (age <= 59) group = "36-59";
            else group = "60+";
            groupMap.get(group).add(appt);
        }

        List<AgeGroupStatsDTO> stats = new ArrayList<>();
        for (String group : List.of("0-17", "18-35", "36-59", "60+")) {
            List<Appointment> groupAppointments = groupMap.get(group);
            int appointmentCount = groupAppointments.size();
            int avgDuration = 0;
            double avgPerPatient = 0.0;
            if (appointmentCount > 0) {
                // Média de duração
                int totalMinutes = groupAppointments.stream()
                        .mapToInt(a -> {
                            LocalTime duration = a.getRecord() != null ? a.getRecord().getDuration() : null;
                            return duration != null ? duration.getHour() * 60 + duration.getMinute() : 0;
                        })
                        .sum();
                avgDuration = (int) Math.round((double) totalMinutes / appointmentCount);
                // Média de consultas por paciente
                Set<String> patientIds = groupAppointments.stream()
                        .map(a -> a.getPatient().getPatientId())
                        .collect(Collectors.toSet());
                if (!patientIds.isEmpty()) {
                    avgPerPatient = (double) appointmentCount / patientIds.size();
                }
            }
            stats.add(new AgeGroupStatsDTO(group, appointmentCount, avgDuration, avgPerPatient));
        }
        return stats;
    }
} 
package com.pcm.psoft.pcmclinic_api.report.service;

import com.pcm.psoft.pcmclinic_api.appointment.model.Appointment;
import com.pcm.psoft.pcmclinic_api.appointment.model.AppointmentStatus;
import com.pcm.psoft.pcmclinic_api.appointment.repository.AppointmentRepository;
import com.pcm.psoft.pcmclinic_api.report.dto.AgeGroupStatsDTO;
import com.pcm.psoft.pcmclinic_api.report.dto.MonthlyReportDTO;
import com.pcm.psoft.pcmclinic_api.report.mapper.ReportMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class ReportService {
    private final AppointmentRepository appointmentRepository;
    private final ReportMapper reportMapper;

    public ReportService(AppointmentRepository appointmentRepository, ReportMapper reportMapper) {
        this.appointmentRepository = appointmentRepository;
        this.reportMapper = reportMapper;
    }

    public MonthlyReportDTO generateMonthlyReport(String monthStr) {
        YearMonth yearMonth = YearMonth.parse(monthStr);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Appointment> appointments = appointmentRepository.findByDateTimeBetween(startOfMonth, endOfMonth);

        int totalAppointments = appointments.size();
        int cancelled = (int) appointments.stream()
                .filter(a -> AppointmentStatus.CANCELED.equals(a.getStatus()))
                .count();
        int rescheduled = (int) appointments.stream()
                .filter(a -> a.isWasRescheduled())
                .count();

        return new MonthlyReportDTO(
                totalAppointments,
                cancelled,
                rescheduled
        );
    }

    public List<AgeGroupStatsDTO> getAppointmentStatsByAgeGroup() {
        List<Appointment> appointments = appointmentRepository.findAll();
        return reportMapper.toAgeGroupStatsDTO(appointments);
    }
} 
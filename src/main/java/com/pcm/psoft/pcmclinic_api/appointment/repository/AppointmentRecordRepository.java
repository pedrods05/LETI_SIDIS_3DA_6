package com.pcm.psoft.pcmclinic_api.appointment.repository;

import com.pcm.psoft.pcmclinic_api.appointment.model.AppointmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppointmentRecordRepository extends JpaRepository<AppointmentRecord, String> {
    Optional<AppointmentRecord> findByAppointment_AppointmentId(String appointmentId);
} 
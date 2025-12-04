package leti_sisdis_6.hapappointmentrecords.repository;

import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppointmentRecordRepository extends JpaRepository<AppointmentRecord, String> {

    // procura por ID da consulta via propriedade aninhada
    Optional<AppointmentRecord> findByAppointment_AppointmentId(String appointmentId);
}

package leti_sisdis_6.hapappointmentrecords.repository;

import leti_sisdis_6.hapappointmentrecords.model.AppointmentProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentProjectionRepository extends JpaRepository<AppointmentProjection, String> {
}


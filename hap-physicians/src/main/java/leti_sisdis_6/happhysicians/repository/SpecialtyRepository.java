package leti_sisdis_6.happhysicians.repository;

import leti_sisdis_6.happhysicians.model.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpecialtyRepository extends JpaRepository<Specialty, String> {
    boolean existsByName(String name);

}

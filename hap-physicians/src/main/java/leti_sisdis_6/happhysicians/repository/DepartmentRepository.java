package leti_sisdis_6.happhysicians.repository;

import leti_sisdis_6.happhysicians.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, String> {
    boolean existsByCode(String code);

}

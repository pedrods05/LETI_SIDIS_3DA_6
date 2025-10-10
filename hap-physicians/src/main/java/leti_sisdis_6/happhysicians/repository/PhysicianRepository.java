package leti_sisdis_6.happhysicians.repository;

import com.pcm.psoft.pcmclinic_api.usermanagement.model.Physician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhysicianRepository extends JpaRepository<Physician, String> {
    boolean existsByUsername(String username);
    boolean existsByLicenseNumber(String licenseNumber);

    @Query("SELECT DISTINCT p FROM Physician p WHERE " +
           "(:name IS NULL OR " +
           "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(p.fullName) LIKE LOWER(CONCAT(:name, '%')) OR " +
           "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name)) OR " +
           "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :word1, '%', :word2, '%')) OR " +
           "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :word2, '%', :word1, '%'))) AND " +
           "(:specialty IS NULL OR LOWER(p.specialty.name) = LOWER(:specialty))")
    List<Physician> searchByNameOrSpecialty(
            @Param("name") String name,
            @Param("word1") String word1,
            @Param("word2") String word2,
            @Param("specialty") String specialty);
}

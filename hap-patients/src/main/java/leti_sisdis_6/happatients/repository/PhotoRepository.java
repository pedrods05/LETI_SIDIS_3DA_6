package leti_sisdis_6.happatients.repository;

import leti_sisdis_6.happatients.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, String> {
} 
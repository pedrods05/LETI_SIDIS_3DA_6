package leti_sisdis_6.happatients.repository;

import leti_sisdis_6.happatients.model.InsuranceInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsuranceInfoRepository extends JpaRepository<InsuranceInfo, String> {
}

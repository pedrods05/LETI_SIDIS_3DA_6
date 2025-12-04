package leti_sisdis_6.happatients.query;
import leti_sisdis_6.happatients.query.PatientSummary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface PatientQueryRepository extends MongoRepository<PatientSummary, String> {
}

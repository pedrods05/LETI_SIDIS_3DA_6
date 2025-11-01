package leti_sisdis_6.happatients.repository;

import leti_sisdis_6.happatients.model.Address;
import leti_sisdis_6.happatients.model.InsuranceInfo;
import leti_sisdis_6.happatients.model.Photo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class JpaSimpleRepositoriesTest {

    @Autowired private AddressRepository addressRepository;
    @Autowired private InsuranceInfoRepository insuranceInfoRepository;
    @Autowired private PhotoRepository photoRepository;

    @Test
    void address_photo_insurance_basic_crud() {
        Address addr = addressRepository.save(Address.builder()
                .id("ADR01").street("S").city("C").postalCode("P").country("PT").build());
        assertThat(addressRepository.findById("ADR01")).isPresent();

        InsuranceInfo ins = insuranceInfoRepository.save(InsuranceInfo.builder()
                .id("INS01").provider("Prov").policyNumber("PN").coverageType("Basic").build());
        assertThat(insuranceInfoRepository.findById("INS01")).isPresent();

        Photo ph = photoRepository.save(Photo.builder()
                .id("PHT01").url("http://img").uploadedAt(LocalDateTime.now()).build());
        assertThat(photoRepository.findById("PHT01")).isPresent();
    }
}


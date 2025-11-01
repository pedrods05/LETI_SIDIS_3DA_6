package leti_sisdis_6.hapappointmentrecords.dto.output;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentRecordViewDTOTest {

    @Test
    @DisplayName("Deve criar AppointmentRecordViewDTO com todos os campos")
    void createViewDTO_withAllFields() {
        // Given
        String recordId = "REC001";
        String appointmentId = "APT001";
        String physicianName = "Dr. João Silva";
        String diagnosis = "Gripe comum";
        String treatmentRecommendations = "Repouso e hidratação";
        String prescriptions = "Paracetamol 1g";
        LocalTime duration = LocalTime.of(0, 30);

        // When
        AppointmentRecordViewDTO viewDTO = AppointmentRecordViewDTO.builder()
                .recordId(recordId)
                .appointmentId(appointmentId)
                .physicianName(physicianName)
                .diagnosis(diagnosis)
                .treatmentRecommendations(treatmentRecommendations)
                .prescriptions(prescriptions)
                .duration(duration)
                .build();

        // Then
        assertThat(viewDTO.getRecordId()).isEqualTo(recordId);
        assertThat(viewDTO.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(viewDTO.getPhysicianName()).isEqualTo(physicianName);
        assertThat(viewDTO.getDiagnosis()).isEqualTo(diagnosis);
        assertThat(viewDTO.getTreatmentRecommendations()).isEqualTo(treatmentRecommendations);
        assertThat(viewDTO.getPrescriptions()).isEqualTo(prescriptions);
        assertThat(viewDTO.getDuration()).isEqualTo(duration);
    }

    @Test
    @DisplayName("Deve criar AppointmentRecordViewDTO usando construtor padrão")
    void createViewDTO_withDefaultConstructor() {
        // When
        AppointmentRecordViewDTO viewDTO = new AppointmentRecordViewDTO();

        // Then
        assertThat(viewDTO.getRecordId()).isNull();
        assertThat(viewDTO.getAppointmentId()).isNull();
        assertThat(viewDTO.getPhysicianName()).isNull();
        assertThat(viewDTO.getDiagnosis()).isNull();
        assertThat(viewDTO.getTreatmentRecommendations()).isNull();
        assertThat(viewDTO.getPrescriptions()).isNull();
        assertThat(viewDTO.getDuration()).isNull();
    }

    @Test
    @DisplayName("Deve criar AppointmentRecordViewDTO usando construtor com argumentos")
    void createViewDTO_withAllArgsConstructor() {
        // Given
        String recordId = "REC002";
        String appointmentId = "APT002";
        String physicianName = "Dra. Maria Santos";
        String diagnosis = "Hipertensão";
        String treatmentRecommendations = "Dieta e exercício";
        String prescriptions = "Losartana 50mg";
        LocalTime duration = LocalTime.of(1, 0);

        // When
        AppointmentRecordViewDTO viewDTO = new AppointmentRecordViewDTO(
                recordId, appointmentId, physicianName, diagnosis, 
                treatmentRecommendations, prescriptions, duration);

        // Then
        assertThat(viewDTO.getRecordId()).isEqualTo(recordId);
        assertThat(viewDTO.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(viewDTO.getPhysicianName()).isEqualTo(physicianName);
        assertThat(viewDTO.getDiagnosis()).isEqualTo(diagnosis);
        assertThat(viewDTO.getTreatmentRecommendations()).isEqualTo(treatmentRecommendations);
        assertThat(viewDTO.getPrescriptions()).isEqualTo(prescriptions);
        assertThat(viewDTO.getDuration()).isEqualTo(duration);
    }

    @Test
    @DisplayName("Deve permitir modificar campos usando setters")
    void modifyViewDTO_usingSetters() {
        // Given
        AppointmentRecordViewDTO viewDTO = new AppointmentRecordViewDTO();
        String newRecordId = "REC003";
        String newAppointmentId = "APT003";
        String newPhysicianName = "Dr. Pedro Costa";
        String newDiagnosis = "Diabetes";
        String newTreatmentRecommendations = "Controle glicêmico";
        String newPrescriptions = "Metformina 850mg";
        LocalTime newDuration = LocalTime.of(0, 45);

        // When
        viewDTO.setRecordId(newRecordId);
        viewDTO.setAppointmentId(newAppointmentId);
        viewDTO.setPhysicianName(newPhysicianName);
        viewDTO.setDiagnosis(newDiagnosis);
        viewDTO.setTreatmentRecommendations(newTreatmentRecommendations);
        viewDTO.setPrescriptions(newPrescriptions);
        viewDTO.setDuration(newDuration);

        // Then
        assertThat(viewDTO.getRecordId()).isEqualTo(newRecordId);
        assertThat(viewDTO.getAppointmentId()).isEqualTo(newAppointmentId);
        assertThat(viewDTO.getPhysicianName()).isEqualTo(newPhysicianName);
        assertThat(viewDTO.getDiagnosis()).isEqualTo(newDiagnosis);
        assertThat(viewDTO.getTreatmentRecommendations()).isEqualTo(newTreatmentRecommendations);
        assertThat(viewDTO.getPrescriptions()).isEqualTo(newPrescriptions);
        assertThat(viewDTO.getDuration()).isEqualTo(newDuration);
    }

    @Test
    @DisplayName("Deve implementar equals e hashCode corretamente")
    void testEqualsAndHashCode() {
        // Given
        LocalTime duration = LocalTime.of(0, 30);
        
        AppointmentRecordViewDTO view1 = AppointmentRecordViewDTO.builder()
                .recordId("REC001")
                .appointmentId("APT001")
                .physicianName("Dr. Test")
                .diagnosis("Test diagnosis")
                .treatmentRecommendations("Test treatment")
                .prescriptions("Test prescription")
                .duration(duration)
                .build();

        AppointmentRecordViewDTO view2 = AppointmentRecordViewDTO.builder()
                .recordId("REC001")
                .appointmentId("APT001")
                .physicianName("Dr. Test")
                .diagnosis("Test diagnosis")
                .treatmentRecommendations("Test treatment")
                .prescriptions("Test prescription")
                .duration(duration)
                .build();

        AppointmentRecordViewDTO view3 = AppointmentRecordViewDTO.builder()
                .recordId("REC002")
                .appointmentId("APT002")
                .physicianName("Dr. Different")
                .diagnosis("Different diagnosis")
                .treatmentRecommendations("Different treatment")
                .prescriptions("Different prescription")
                .duration(LocalTime.of(1, 0))
                .build();

        // Then
        assertThat(view1).isEqualTo(view2);
        assertThat(view1).isNotEqualTo(view3);
        assertThat(view1.hashCode()).isEqualTo(view2.hashCode());
    }

    @Test
    @DisplayName("Deve implementar toString corretamente")
    void testToString() {
        // Given
        AppointmentRecordViewDTO viewDTO = AppointmentRecordViewDTO.builder()
                .recordId("REC123")
                .appointmentId("APT456")
                .physicianName("Dr. João Silva")
                .diagnosis("Gripe")
                .treatmentRecommendations("Repouso")
                .prescriptions("Paracetamol")
                .duration(LocalTime.of(0, 30))
                .build();

        // When
        String toString = viewDTO.toString();

        // Then
        assertThat(toString).contains("recordId=REC123");
        assertThat(toString).contains("appointmentId=APT456");
        assertThat(toString).contains("physicianName=Dr. João Silva");
        assertThat(toString).contains("diagnosis=Gripe");
        assertThat(toString).contains("treatmentRecommendations=Repouso");
        assertThat(toString).contains("prescriptions=Paracetamol");
        assertThat(toString).contains("duration=00:30");
    }

    @Test
    @DisplayName("Deve permitir campos nulos")
    void allowNullFields() {
        // When
        AppointmentRecordViewDTO viewDTO = AppointmentRecordViewDTO.builder()
                .recordId(null)
                .appointmentId(null)
                .physicianName(null)
                .diagnosis(null)
                .treatmentRecommendations(null)
                .prescriptions(null)
                .duration(null)
                .build();

        // Then
        assertThat(viewDTO.getRecordId()).isNull();
        assertThat(viewDTO.getAppointmentId()).isNull();
        assertThat(viewDTO.getPhysicianName()).isNull();
        assertThat(viewDTO.getDiagnosis()).isNull();
        assertThat(viewDTO.getTreatmentRecommendations()).isNull();
        assertThat(viewDTO.getPrescriptions()).isNull();
        assertThat(viewDTO.getDuration()).isNull();
    }

    @Test
    @DisplayName("Deve trabalhar com diferentes durações")
    void workWithDifferentDurations() {
        // Given
        LocalTime shortDuration = LocalTime.of(0, 15);
        LocalTime mediumDuration = LocalTime.of(0, 30);
        LocalTime longDuration = LocalTime.of(1, 30);

        // When
        AppointmentRecordViewDTO shortView = AppointmentRecordViewDTO.builder()
                .duration(shortDuration)
                .build();
        
        AppointmentRecordViewDTO mediumView = AppointmentRecordViewDTO.builder()
                .duration(mediumDuration)
                .build();
        
        AppointmentRecordViewDTO longView = AppointmentRecordViewDTO.builder()
                .duration(longDuration)
                .build();

        // Then
        assertThat(shortView.getDuration()).isEqualTo(shortDuration);
        assertThat(mediumView.getDuration()).isEqualTo(mediumDuration);
        assertThat(longView.getDuration()).isEqualTo(longDuration);
    }
}

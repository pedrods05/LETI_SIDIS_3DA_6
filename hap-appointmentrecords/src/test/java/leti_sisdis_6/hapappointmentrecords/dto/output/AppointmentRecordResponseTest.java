package leti_sisdis_6.hapappointmentrecords.dto.output;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentRecordResponseTest {

    @Test
    @DisplayName("Deve criar AppointmentRecordResponse com todos os campos")
    void createResponse_withAllFields() {
        // Given
        String message = "Appointment record created successfully";
        String appointmentId = "APT001";
        String recordId = "REC001";

        // When
        AppointmentRecordResponse response = AppointmentRecordResponse.builder()
                .message(message)
                .appointmentId(appointmentId)
                .recordId(recordId)
                .build();

        // Then
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(response.getRecordId()).isEqualTo(recordId);
    }

    @Test
    @DisplayName("Deve criar AppointmentRecordResponse usando construtor padrão")
    void createResponse_withDefaultConstructor() {
        // When
        AppointmentRecordResponse response = new AppointmentRecordResponse();

        // Then
        assertThat(response.getMessage()).isNull();
        assertThat(response.getAppointmentId()).isNull();
        assertThat(response.getRecordId()).isNull();
    }

    @Test
    @DisplayName("Deve criar AppointmentRecordResponse usando construtor com argumentos")
    void createResponse_withAllArgsConstructor() {
        // Given
        String message = "Record updated";
        String appointmentId = "APT002";
        String recordId = "REC002";

        // When
        AppointmentRecordResponse response = new AppointmentRecordResponse(message, appointmentId, recordId);

        // Then
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(response.getRecordId()).isEqualTo(recordId);
    }

    @Test
    @DisplayName("Deve permitir modificar campos usando setters")
    void modifyResponse_usingSetters() {
        // Given
        AppointmentRecordResponse response = new AppointmentRecordResponse();
        String newMessage = "Updated message";
        String newAppointmentId = "APT003";
        String newRecordId = "REC003";

        // When
        response.setMessage(newMessage);
        response.setAppointmentId(newAppointmentId);
        response.setRecordId(newRecordId);

        // Then
        assertThat(response.getMessage()).isEqualTo(newMessage);
        assertThat(response.getAppointmentId()).isEqualTo(newAppointmentId);
        assertThat(response.getRecordId()).isEqualTo(newRecordId);
    }

    @Test
    @DisplayName("Deve implementar equals e hashCode corretamente")
    void testEqualsAndHashCode() {
        // Given
        AppointmentRecordResponse response1 = AppointmentRecordResponse.builder()
                .message("Success")
                .appointmentId("APT001")
                .recordId("REC001")
                .build();

        AppointmentRecordResponse response2 = AppointmentRecordResponse.builder()
                .message("Success")
                .appointmentId("APT001")
                .recordId("REC001")
                .build();

        AppointmentRecordResponse response3 = AppointmentRecordResponse.builder()
                .message("Different")
                .appointmentId("APT002")
                .recordId("REC002")
                .build();

        // Then
        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(response3);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    @DisplayName("Deve implementar toString corretamente")
    void testToString() {
        // Given
        AppointmentRecordResponse response = AppointmentRecordResponse.builder()
                .message("Test message")
                .appointmentId("APT123")
                .recordId("REC456")
                .build();

        // When
        String toString = response.toString();

        // Then
        assertThat(toString).contains("message=Test message");
        assertThat(toString).contains("appointmentId=APT123");
        assertThat(toString).contains("recordId=REC456");
    }

    @Test
    @DisplayName("Deve permitir campos nulos")
    void allowNullFields() {
        // When
        AppointmentRecordResponse response = AppointmentRecordResponse.builder()
                .message(null)
                .appointmentId(null)
                .recordId(null)
                .build();

        // Then
        assertThat(response.getMessage()).isNull();
        assertThat(response.getAppointmentId()).isNull();
        assertThat(response.getRecordId()).isNull();
    }

    @Test
    @DisplayName("Deve criar resposta de sucesso")
    void createSuccessResponse() {
        // Given
        String appointmentId = "APT789";
        String recordId = "REC789";

        // When
        AppointmentRecordResponse response = AppointmentRecordResponse.builder()
                .message("Appointment record created successfully")
                .appointmentId(appointmentId)
                .recordId(recordId)
                .build();

        // Then
        assertThat(response.getMessage()).contains("successfully");
        assertThat(response.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(response.getRecordId()).isEqualTo(recordId);
    }
}

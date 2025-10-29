package leti_sisdis_6.hapappointmentrecords.setup;

import leti_sisdis_6.hapappointmentrecords.model.Appointment;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecord;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentStatus;
import leti_sisdis_6.hapappointmentrecords.model.ConsultationType;

import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRepository;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRecordRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Order(2)
public class DataBootstrap implements CommandLineRunner {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentRecordRepository appointmentRecordRepository;

    @Override
    public void run(String... args) {
        if (appointmentRepository.count() > 0) return; // idempotente

        // IDs que já semeaste noutros serviços:
        List<String> patientIds = Arrays.asList("PAT01", "PAT02");
        List<String> physicianIds = Arrays.asList("PHY01", "PHY02");

        String[] diagnoses = {
                "Hipertensão arterial", "Diabetes tipo 2", "Artrite reumatoide",
                "Asma brônquica", "Depressão", "Ansiedade",
                "Dor lombar crônica", "Enxaqueca", "Gastrite", "Insônia"
        };

        String[] treatments = {
                "Losartana 50mg, 1x/dia", "Metformina 850mg, 2x/dia",
                "Ibuprofeno 600mg, 3x/dia", "Salbutamol conforme necessidade",
                "Sertralina 50mg, 1x/dia", "Alprazolam 0.25mg, 2x/dia",
                "Fisioterapia 2x/semana", "Sumatriptana 50mg conforme necessidade",
                "Omeprazol 20mg, 1x/dia", "Higiene do sono"
        };

        String[] recommendations = {
                "Rever em 3 meses", "Rever em 1 mês", "Rever em 2 meses",
                "Rever em 6 meses", "Acompanhamento em 1 mês", "Avaliar em 2 semanas",
                "Exercícios + rever em 1 mês", "Diário de crises + 2 meses",
                "Evitar ácidos + 3 meses", "Diário do sono + 1 mês"
        };

        AtomicInteger counter = new AtomicInteger(1);

        for (String patId : patientIds) {
            for (int i = 0; i < 5; i++) {
                String phyId = physicianIds.get(i % physicianIds.size());

                LocalDateTime dt = LocalDateTime.now()
                        .minusMonths(5 - i)
                        .withHour(9 + (i % 4))
                        .withMinute(0).withSecond(0).withNano(0);

                int n = counter.getAndIncrement();

                Appointment appt = Appointment.builder()
                        .appointmentId(String.format("APT%02d", n))
                        .patientId(patId)         // <- referencia por ID, não objeto
                        .physicianId(phyId)       // <- referencia por ID, não objeto
                        .dateTime(dt)
                        .consultationType(i == 0 ? ConsultationType.FIRST_TIME : ConsultationType.FOLLOW_UP)
                        .status(AppointmentStatus.COMPLETED)
                        .build();

                appt = appointmentRepository.save(appt);

                int idx = i % diagnoses.length;

                AppointmentRecord rec = AppointmentRecord.builder()
                        .recordId(String.format("REC%02d", n))
                        .appointment(appt)
                        // .appointment(appt) // se o teu modelo liga por objeto, usa esta linha e remove a de cima
                        .diagnosis(diagnoses[idx])
                        .treatmentRecommendations(recommendations[idx])
                        .prescriptions(treatments[idx])
                        .duration(LocalTime.of(0, 20))
                        .build();

                appointmentRecordRepository.save(rec);
            }
        }
    }
}

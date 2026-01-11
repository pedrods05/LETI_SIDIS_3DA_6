package leti_sisdis_6.happhysicians.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderHandler {

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.appointment.reminders.${spring.profiles.active}", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-exchange}", type = "topic"),
            key = "appointment.reminder"
    ))
    public void handleAppointmentReminder(AppointmentReminderEvent event) {
        log.info("📧 [Reminder Handler] Processando lembrete de appointment: {}", event.getAppointmentId());
        log.info("   Tipo: {}", event.getReminderType());
        log.info("   Paciente: {} ({})", event.getPatientName(), event.getPatientEmail());
        log.info("   Médico: {}", event.getPhysicianName());
        log.info("   Data/Hora: {}", event.getDateTime());

        // Simular envio de email/SMS (em produção, integrar com serviço de email/SMS)
        try {
            sendReminderEmail(event);
            log.info("✅ [Reminder Handler] Lembrete enviado com sucesso para: {}", event.getPatientEmail());
        } catch (Exception e) {
            log.error("⚠️ [Reminder Handler] Falha ao enviar lembrete: {}", e.getMessage(), e);
        }
    }

    private void sendReminderEmail(AppointmentReminderEvent event) {
        // Simulação de envio de email
        // Em produção, integrar com serviço de email (ex: SendGrid, AWS SES, etc.)
        String subject = "Lembrete de Consulta - " + event.getReminderType();
        String body = String.format(
            "Olá %s,\n\n" +
            "Este é um lembrete sobre sua consulta:\n\n" +
            "Médico: %s\n" +
            "Data/Hora: %s\n" +
            "Tipo: %s\n\n" +
            "Por favor, confirme sua presença.\n\n" +
            "Atenciosamente,\n" +
            "Sistema HAP",
            event.getPatientName(),
            event.getPhysicianName(),
            event.getDateTime(),
            event.getConsultationType()
        );

        // Log do email (em produção, enviar realmente)
        log.info("📨 [Email] Para: {}", event.getPatientEmail());
        log.info("📨 [Email] Assunto: {}", subject);
        log.info("📨 [Email] Corpo: {}", body);
    }
}


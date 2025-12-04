package leti_sisdis_6.happhysicians.events;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentReminderHandler {

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.appointment.reminders", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-exchange}", type = "topic"),
            key = "appointment.reminder"
    ))
    public void handleAppointmentReminder(AppointmentReminderEvent event) {
        System.out.println("📧 [Reminder Handler] Processando lembrete de appointment: " + event.getAppointmentId());
        System.out.println("   Tipo: " + event.getReminderType());
        System.out.println("   Paciente: " + event.getPatientName() + " (" + event.getPatientEmail() + ")");
        System.out.println("   Médico: " + event.getPhysicianName());
        System.out.println("   Data/Hora: " + event.getDateTime());

        // Simular envio de email/SMS (em produção, integrar com serviço de email/SMS)
        try {
            sendReminderEmail(event);
            System.out.println("✅ [Reminder Handler] Lembrete enviado com sucesso para: " + event.getPatientEmail());
        } catch (Exception e) {
            System.err.println("⚠️ [Reminder Handler] Falha ao enviar lembrete: " + e.getMessage());
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
        System.out.println("📨 [Email] Para: " + event.getPatientEmail());
        System.out.println("📨 [Email] Assunto: " + subject);
        System.out.println("📨 [Email] Corpo: " + body);
    }
}


package leti_sisdis_6.happhysicians.command;

import leti_sisdis_6.happhysicians.dto.request.RegisterPhysicianRequest;
import leti_sisdis_6.happhysicians.dto.request.UpdatePhysicianRequest;
import leti_sisdis_6.happhysicians.dto.response.PhysicianIdResponse;
import leti_sisdis_6.happhysicians.dto.response.PhysicianFullDTO;
import leti_sisdis_6.happhysicians.events.PhysicianRegisteredEvent;
import leti_sisdis_6.happhysicians.events.PhysicianUpdatedEvent;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import leti_sisdis_6.happhysicians.services.PhysicianService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhysicianCommandService {

    private final PhysicianService physicianService;
    private final PhysicianRepository physicianRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${hap.rabbitmq.exchange:hap-exchange}")
    private String exchangeName;

    @Transactional
    public PhysicianIdResponse registerPhysician(RegisterPhysicianRequest request) {
        // Delegate to existing service
        PhysicianIdResponse response = physicianService.register(request);
        
        // Publish event
        publishPhysicianRegisteredEvent(response.getPhysicianId());
        
        return response;
    }

    @Transactional
    public PhysicianFullDTO updatePhysician(String physicianId, UpdatePhysicianRequest request) {
        // Delegate to existing service
        PhysicianFullDTO response = physicianService.partialUpdate(physicianId, request);
        
        // Publish event
        publishPhysicianUpdatedEvent(physicianId);
        
        return response;
    }

    private void publishPhysicianRegisteredEvent(String physicianId) {
        try {
            Physician physician = physicianRepository.findById(physicianId)
                    .orElseThrow(() -> new RuntimeException("Physician not found after registration"));

            PhysicianRegisteredEvent event = new PhysicianRegisteredEvent(
                    physician.getPhysicianId(),
                    physician.getFullName(),
                    physician.getLicenseNumber(),
                    physician.getUsername(),
                    physician.getSpecialty() != null ? physician.getSpecialty().getSpecialtyId() : null,
                    physician.getSpecialty() != null ? physician.getSpecialty().getName() : null,
                    physician.getDepartment() != null ? physician.getDepartment().getDepartmentId() : null,
                    physician.getDepartment() != null ? physician.getDepartment().getName() : null
            );

            rabbitTemplate.convertAndSend(exchangeName, "physician.registered", event);
            System.out.println("⚡ Evento PhysicianRegisteredEvent enviado para o RabbitMQ: " + physicianId);
        } catch (Exception e) {
            System.err.println("⚠️ FALHA ao enviar evento RabbitMQ: " + e.getMessage());
        }
    }

    private void publishPhysicianUpdatedEvent(String physicianId) {
        try {
            Physician physician = physicianRepository.findById(physicianId)
                    .orElseThrow(() -> new RuntimeException("Physician not found after update"));

            PhysicianUpdatedEvent event = new PhysicianUpdatedEvent(
                    physician.getPhysicianId(),
                    physician.getFullName(),
                    physician.getLicenseNumber(),
                    physician.getUsername(),
                    physician.getSpecialty() != null ? physician.getSpecialty().getSpecialtyId() : null,
                    physician.getSpecialty() != null ? physician.getSpecialty().getName() : null,
                    physician.getDepartment() != null ? physician.getDepartment().getDepartmentId() : null,
                    physician.getDepartment() != null ? physician.getDepartment().getName() : null
            );

            rabbitTemplate.convertAndSend(exchangeName, "physician.updated", event);
            System.out.println("⚡ Evento PhysicianUpdatedEvent enviado para o RabbitMQ: " + physicianId);
        } catch (Exception e) {
            System.err.println("⚠️ FALHA ao enviar evento RabbitMQ: " + e.getMessage());
        }
    }
}


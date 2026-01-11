package leti_sisdis_6.happhysicians.services;

import leti_sisdis_6.happhysicians.dto.request.RegisterPhysicianRequest;
import leti_sisdis_6.happhysicians.dto.request.UpdatePhysicianRequest;
import leti_sisdis_6.happhysicians.dto.response.PhysicianIdResponse;
import leti_sisdis_6.happhysicians.api.PhysicianMapper;
import leti_sisdis_6.happhysicians.dto.response.PhysicianLimitedDTO;
import leti_sisdis_6.happhysicians.dto.response.PhysicianFullDTO;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.exceptions.NotFoundException;
import leti_sisdis_6.happhysicians.exceptions.MicroserviceCommunicationException;
import leti_sisdis_6.happhysicians.model.*;
import leti_sisdis_6.happhysicians.repository.DepartmentRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import leti_sisdis_6.happhysicians.repository.SpecialtyRepository;
import leti_sisdis_6.happhysicians.services.ExternalServiceClient;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import leti_sisdis_6.happhysicians.dto.response.TopPhysicianDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhysicianService {

    private final PhysicianRepository physicianRepository;
    private final DepartmentRepository departmentRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhysicianMapper physicianMapper;
    private final AppointmentRepository appointmentRepository;

    @Autowired
    private ExternalServiceClient externalServiceClient;

    @Transactional
    public PhysicianIdResponse register(RegisterPhysicianRequest request) {
        if (physicianRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already in use");
        }

        if (physicianRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new IllegalArgumentException("License number already in use");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                .orElseThrow(() -> new EntityNotFoundException("Specialty not found"));

        String physicianId = generatePhysicianId();

        // Create auth user via hap-auth API
        Map<String, String> authRequest = new HashMap<>();
        authRequest.put("username", request.getUsername());
        authRequest.put("password", request.getPassword());
        authRequest.put("role", "PHYSICIAN");
        
        try {
            // Register user in hap-auth service
            externalServiceClient.registerUser(request.getUsername(), request.getPassword(), "PHYSICIAN");
            log.info("Successfully registered physician in auth service: {}", request.getUsername());
        } catch (Exception e) {
            // Log warning but continue with local registration
            log.warn("Warning: Could not register physician in auth service: {}", e.getMessage());
        }

        Physician physician = physicianMapper.toEntity(request, department, specialty);
        physician.setPhysicianId(physicianId);
        physician.setPassword(passwordEncoder.encode(request.getPassword()));

        physicianRepository.save(physician);

        return new PhysicianIdResponse(physicianId, "Physician registered successfully.", null);
    }

    @Transactional
    public PhysicianIdResponse registerWithPhoto(RegisterPhysicianRequest request, MultipartFile photoFile) {
        if (physicianRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already in use");
        }
        if (physicianRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new IllegalArgumentException("License number already in use");
        }
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));
        Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                .orElseThrow(() -> new EntityNotFoundException("Specialty not found"));
        String physicianId = generatePhysicianId();
        Physician physician = physicianMapper.toEntity(request, department, specialty);
        physician.setPhysicianId(physicianId);
        physician.setPassword(passwordEncoder.encode(request.getPassword()));
        Physician.PhotoInfo photoInfo = null;
        PhysicianIdResponse.PhotoDTO photoDTO = null;
        if (photoFile != null && !photoFile.isEmpty()) {
            try {
                String folder = "photos/";
                Files.createDirectories(Paths.get(folder));
                String filename = physicianId + "_" + System.currentTimeMillis() + "_" + photoFile.getOriginalFilename();
                Path filePath = Paths.get(folder, filename);
                Files.write(filePath, photoFile.getBytes());
                String url = "/" + folder + filename;
                LocalDateTime uploadedAt = LocalDateTime.now();
                photoInfo = new Physician.PhotoInfo(url, uploadedAt);
                photoDTO = new PhysicianIdResponse.PhotoDTO(url, uploadedAt);
                physician.setPhoto(photoInfo);
            } catch (Exception e) {
                throw new RuntimeException("Failed to store photo", e);
            }
        }
        physicianRepository.save(physician);
        return new PhysicianIdResponse(physicianId, "Physician registered successfully.", photoDTO);
    }

    @Transactional(readOnly = true)
    public PhysicianFullDTO getPhysicianDetails(String id) {
        Physician physician = physicianRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Physician not found with id: " + id));

        return physicianMapper.toFullDTO(physician);
    }

    private String generatePhysicianId() {
        List<String> ids = physicianRepository.findAll().stream()
                .map(Physician::getPhysicianId)
                .filter(id -> id.startsWith("PHY"))
                .toList();

        int max = ids.stream()
                .map(id -> id.substring(3))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        return String.format("PHY%02d", max + 1);
    }
    public List<PhysicianLimitedDTO> searchByNameOrSpecialty(String name, String specialty) {
        String word1 = null;
        String word2 = null;

        if (name != null) {
            name = name.trim();
            name = name.replaceAll("\\s+", " ");

            if (name.length() < 3) {
                throw new IllegalArgumentException("Name search requires at least 3 characters");
            }

            // Split the name into words
            String[] words = name.split("\\s+");
            if (words.length >= 2) {
                word1 = words[0];
                word2 = words[words.length - 1];
            }
        }

        if (specialty != null) {
            specialty = specialty.trim();
            specialty = specialty.replaceAll("\\s+", " ");
        }

        List<Physician> physicians = physicianRepository.searchByNameOrSpecialty(name, word1, word2, specialty);

        if (physicians.isEmpty()) {
            throw new NotFoundException("No physicians found with the given criteria");
        }

        return physicianMapper.toLimitedDTOList(physicians);
    }

    @Transactional
    public PhysicianFullDTO partialUpdate(String id, UpdatePhysicianRequest request) {
        Physician physician = physicianRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Physician not found with id: " + id));

        // Não permitir alteração de username ou password

        // Atualizar fullName
        if (request.getFullName() != null) {
            physician.setFullName(request.getFullName());
        }
        // Atualizar licenseNumber (validar unicidade)
        if (request.getLicenseNumber() != null && !request.getLicenseNumber().equals(physician.getLicenseNumber())) {
            if (physicianRepository.existsByLicenseNumber(request.getLicenseNumber())) {
                throw new IllegalArgumentException("License number already in use");
            }
            physician.setLicenseNumber(request.getLicenseNumber());
        }
        // Atualizar specialty
        if (request.getSpecialtyId() != null) {
            Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                    .orElseThrow(() -> new EntityNotFoundException("Specialty not found"));
            physician.setSpecialty(specialty);
        }
        // Atualizar department
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new EntityNotFoundException("Department not found"));
            physician.setDepartment(department);
        }
        // Atualizar emails
        if (request.getEmails() != null) {
            physician.setEmails(request.getEmails());
        }
        // Atualizar phoneNumbers
        if (request.getPhoneNumbers() != null) {
            physician.setPhoneNumbers(request.getPhoneNumbers());
        }
        // Atualizar workingHourStart
        if (request.getWorkingHourStart() != null) {
            physician.setWorkingHourStart(LocalTime.parse(request.getWorkingHourStart()));
        }
        // Atualizar workingHourEnd
        if (request.getWorkingHourEnd() != null) {
            physician.setWorkingHourEnd(LocalTime.parse(request.getWorkingHourEnd()));
        }
        physicianRepository.save(physician);
        return physicianMapper.toFullDTO(physician);
    }

    @Transactional(readOnly = true)
    public List<TopPhysicianDTO> getTop5Physicians(LocalDateTime from, LocalDateTime to) {
        List<Object[]> results = appointmentRepository.findTop5PhysiciansByAppointmentCount(from, to);
        return results.stream().limit(5).map(obj -> TopPhysicianDTO.builder()
                .physicianId((String) obj[0])
                .fullName((String) obj[1])
                .specialtyName((String) obj[2])
                .appointmentCount((Long) obj[3])
                .build()
        ).toList();
    }

    // ===== MÉTODOS DE COMUNICAÇÃO INTER-MICROSERVIÇOS =====

    /**
     * Get physician details with patient statistics from other microservices
     */
    @Transactional(readOnly = true)
    public PhysicianFullDTO getPhysicianWithStats(String id) {
        Physician physician = physicianRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Physician not found with id: " + id));

        PhysicianFullDTO physicianDTO = physicianMapper.toFullDTO(physician);

        // Get appointment records from hap-appointmentrecords
        try {
            List<Map<String, Object>> appointmentRecords = externalServiceClient.getAppointmentRecordsByPhysician(id);
            // Add statistics to the DTO if needed
            // physicianDTO.setAppointmentRecordsCount(appointmentRecords.size());
        } catch (Exception e) {
            // Log warning but continue without external data
            log.warn("Warning: Could not fetch appointment records for physician {}: {}", id, e.getMessage());
        }

        return physicianDTO;
    }

    /**
     * Validate physician token with hap-auth service
     */
    public boolean validatePhysicianToken(String token) {
        try {
            externalServiceClient.validateToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get physician's patient list from appointments
     */
    @Transactional(readOnly = true)
    public List<String> getPhysicianPatientIds(String physicianId) {
        List<Appointment> appointments = appointmentRepository.findByPhysicianPhysicianId(physicianId);
        return appointments.stream()
                .map(Appointment::getPatientId)
                .distinct()
                .toList();
    }
}

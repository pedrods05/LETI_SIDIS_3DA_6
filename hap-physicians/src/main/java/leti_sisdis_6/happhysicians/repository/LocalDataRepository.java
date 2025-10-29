package leti_sisdis_6.happhysicians.repository;

import leti_sisdis_6.happhysicians.model.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class LocalDataRepository {
    
    // Data stores
    private final Map<String, Physician> physicians = new ConcurrentHashMap<>();
    private final Map<String, Appointment> appointments = new ConcurrentHashMap<>();
    private final Map<String, Department> departments = new ConcurrentHashMap<>();
    private final Map<String, Specialty> specialties = new ConcurrentHashMap<>();
    
    // ===== PHYSICIAN OPERATIONS =====
    
    public Optional<Physician> findPhysicianById(String id) {
        return Optional.ofNullable(physicians.get(id));
    }
    
    public Physician savePhysician(Physician physician) {
        if (physician == null || physician.getPhysicianId() == null) {
            throw new IllegalArgumentException("Physician or physicianId cannot be null");
        }
        physicians.put(physician.getPhysicianId(), physician);
        return physician;
    }
    
    public List<Physician> findAllPhysicians() {
        return new ArrayList<>(physicians.values());
    }
    
    public void deletePhysicianById(String id) {
        physicians.remove(id);
    }
    
    public boolean existsPhysicianById(String id) {
        return physicians.containsKey(id);
    }
    
    public boolean existsPhysicianByUsername(String username) {
        return physicians.values().stream()
                .anyMatch(physician -> physician.getUsername().equals(username));
    }
    
    public boolean existsPhysicianByLicenseNumber(String licenseNumber) {
        return physicians.values().stream()
                .anyMatch(physician -> physician.getLicenseNumber().equals(licenseNumber));
    }
    
    public Optional<Physician> findPhysicianByUsername(String username) {
        return physicians.values().stream()
                .filter(physician -> physician.getUsername().equals(username))
                .findFirst();
    }
    
    public List<Physician> findPhysiciansBySpecialtyId(String specialtyId) {
        return physicians.values().stream()
                .filter(physician -> physician.getSpecialty().getSpecialtyId().equals(specialtyId))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<Physician> findPhysiciansByDepartmentId(String departmentId) {
        return physicians.values().stream()
                .filter(physician -> physician.getDepartment().getDepartmentId().equals(departmentId))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<Physician> searchPhysiciansByNameOrSpecialty(String name, String specialty) {
        return physicians.values().stream()
                .filter(physician -> {
                    boolean nameMatch = name == null || 
                        physician.getFullName().toLowerCase().contains(name.toLowerCase());
                    boolean specialtyMatch = specialty == null || 
                        physician.getSpecialty().getName().toLowerCase().equals(specialty.toLowerCase());
                    return nameMatch && specialtyMatch;
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    // ===== APPOINTMENT OPERATIONS =====
    
    public Optional<Appointment> findAppointmentById(String id) {
        return Optional.ofNullable(appointments.get(id));
    }
    
    public Appointment saveAppointment(Appointment appointment) {
        if (appointment == null || appointment.getAppointmentId() == null) {
            throw new IllegalArgumentException("Appointment or appointmentId cannot be null");
        }
        appointments.put(appointment.getAppointmentId(), appointment);
        return appointment;
    }
    
    public List<Appointment> findAllAppointments() {
        return new ArrayList<>(appointments.values());
    }
    
    public void deleteAppointmentById(String id) {
        appointments.remove(id);
    }
    
    public boolean existsAppointmentById(String id) {
        return appointments.containsKey(id);
    }
    
    public boolean existsAppointmentByPhysicianIdAndDateTime(String physicianId, LocalDateTime dateTime) {
        return appointments.values().stream()
                .anyMatch(appointment ->
                    appointment.getPhysician().getPhysicianId().equals(physicianId) &&
                    appointment.getDateTime().equals(dateTime)
                );
    }
    
    public List<Appointment> findAppointmentsByPatientId(String patientId) {
        return appointments.values().stream()
                .filter(appointment -> appointment.getPatientId().equals(patientId))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<Appointment> findAppointmentsByPatientIdOrderByDateTimeDesc(String patientId) {
        return appointments.values().stream()
                .filter(appointment -> appointment.getPatientId().equals(patientId))
                .sorted((a, b) -> b.getDateTime().compareTo(a.getDateTime()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<Appointment> findAppointmentsByDateTimeAfterOrderByDateTimeAsc(LocalDateTime dateTime) {
        return appointments.values().stream()
                .filter(appointment -> appointment.getDateTime().isAfter(dateTime))
                .sorted(Comparator.comparing(Appointment::getDateTime))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<Appointment> findAppointmentsByPhysicianIdAndDateTimeBetween(String physicianId, LocalDateTime start, LocalDateTime end) {
        return appointments.values().stream()
                .filter(appointment ->
                    appointment.getPhysician().getPhysicianId().equals(physicianId) &&
                    !appointment.getDateTime().isBefore(start) &&
                    !appointment.getDateTime().isAfter(end)
                )
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<Appointment> findAppointmentsByDateTimeBetween(LocalDateTime start, LocalDateTime end) {
        return appointments.values().stream()
                .filter(appointment ->
                    !appointment.getDateTime().isBefore(start) &&
                    !appointment.getDateTime().isAfter(end)
                )
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<Appointment> findAppointmentsByPatientIdAndStatus(String patientId, AppointmentStatus status) {
        return appointments.values().stream()
                .filter(appointment ->
                    appointment.getPatientId().equals(patientId) &&
                    appointment.getStatus().equals(status)
                )
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<Appointment> findAppointmentsByPhysicianId(String physicianId) {
        return appointments.values().stream()
                .filter(appointment -> appointment.getPhysician().getPhysicianId().equals(physicianId))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<Object[]> findTop5PhysiciansByAppointmentCount(LocalDateTime from, LocalDateTime to) {
        Map<String, Long> physicianCounts = appointments.values().stream()
                .filter(appointment ->
                    !appointment.getDateTime().isBefore(from) &&
                    !appointment.getDateTime().isAfter(to)
                )
                .collect(Collectors.groupingBy(
                    appointment -> appointment.getPhysician().getPhysicianId(),
                    Collectors.counting()
                ));

        return physicianCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> new Object[]{
                    entry.getKey(),
                    appointments.values().stream()
                            .filter(app -> app.getPhysician().getPhysicianId().equals(entry.getKey()))
                            .findFirst()
                            .map(app -> app.getPhysician().getFullName())
                            .orElse("Unknown"),
                    appointments.values().stream()
                            .filter(app -> app.getPhysician().getPhysicianId().equals(entry.getKey()))
                            .findFirst()
                            .map(app -> app.getPhysician().getSpecialty().getName())
                            .orElse("Unknown"),
                    entry.getValue()
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    // ===== DEPARTMENT OPERATIONS =====
    
    public Optional<Department> findDepartmentById(String id) {
        return Optional.ofNullable(departments.get(id));
    }
    
    public Department saveDepartment(Department department) {
        if (department == null || department.getDepartmentId() == null) {
            throw new IllegalArgumentException("Department or departmentId cannot be null");
        }
        departments.put(department.getDepartmentId(), department);
        return department;
    }
    
    public List<Department> findAllDepartments() {
        return new ArrayList<>(departments.values());
    }
    
    public void deleteDepartmentById(String id) {
        departments.remove(id);
    }
    
    public boolean existsDepartmentById(String id) {
        return departments.containsKey(id);
    }
    
    public boolean existsDepartmentByCode(String code) {
        return departments.values().stream()
                .anyMatch(department -> department.getCode().equals(code));
    }
    
    // ===== SPECIALTY OPERATIONS =====
    
    public Optional<Specialty> findSpecialtyById(String id) {
        return Optional.ofNullable(specialties.get(id));
    }
    
    public Specialty saveSpecialty(Specialty specialty) {
        if (specialty == null || specialty.getSpecialtyId() == null) {
            throw new IllegalArgumentException("Specialty or specialtyId cannot be null");
        }
        specialties.put(specialty.getSpecialtyId(), specialty);
        return specialty;
    }
    
    public List<Specialty> findAllSpecialties() {
        return new ArrayList<>(specialties.values());
    }
    
    public void deleteSpecialtyById(String id) {
        specialties.remove(id);
    }
    
    public boolean existsSpecialtyById(String id) {
        return specialties.containsKey(id);
    }
    
    public boolean existsSpecialtyByName(String name) {
        return specialties.values().stream()
                .anyMatch(specialty -> specialty.getName().equals(name));
    }
    
    // ===== UTILITY OPERATIONS =====
    
    public void clearAll() {
        physicians.clear();
        appointments.clear();
        departments.clear();
        specialties.clear();
    }
    
    public Map<String, Integer> getDataCounts() {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("physicians", physicians.size());
        counts.put("appointments", appointments.size());
        counts.put("departments", departments.size());
        counts.put("specialties", specialties.size());
        return counts;
    }
}

package leti_sisdis_6.happhysicians.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "physicians")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Physician {

    @Id
    @Column(nullable = false, length = 10)
    private String physicianId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String licenseNumber;

    @Column(nullable = false, unique = true)
    private String username; // email

    @Column(nullable = false)
    private String password;

    @ManyToOne(optional = false)
    @JoinColumn(name = "specialty_id")
    private Specialty specialty;

    @ManyToOne(optional = false)
    @JoinColumn(name = "department_id")
    private Department department;

    @ElementCollection
    @CollectionTable(name = "physician_emails", joinColumns = @JoinColumn(name = "physician_id"))
    @Column(name = "email")
    private List<String> emails;

    @ElementCollection
    @CollectionTable(name = "physician_phones", joinColumns = @JoinColumn(name = "physician_id"))
    @Column(name = "phone_number")
    private List<String> phoneNumbers;

    @Column(nullable = false)
    private LocalTime workingHourStart;

    @Column(nullable = false)
    private LocalTime workingHourEnd;

    @Embedded
    private PhotoInfo photo;

    @Embeddable
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class PhotoInfo {
        private String url;
        private java.time.LocalDateTime uploadedAt;
    }
}

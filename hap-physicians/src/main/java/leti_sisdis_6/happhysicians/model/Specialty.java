package leti_sisdis_6.happhysicians.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "specialties")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Specialty {

    @Id
    private String specialtyId;

    @Column(nullable = false, unique = true)
    private String name;
}

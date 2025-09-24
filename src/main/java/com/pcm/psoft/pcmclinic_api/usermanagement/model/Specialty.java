package com.pcm.psoft.pcmclinic_api.usermanagement.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "specialties")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Specialty {

    @Id
    private String specialtyId;

    @Column(nullable = false, unique = true)
    private String name;
}

package com.pcm.psoft.pcmclinic_api.usermanagement.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "departments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Department {

    @Id
    private String departmentId;

    @Column(length = 5, nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;
}

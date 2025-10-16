# HAP-PHYSICIANS

## Overview

O módulo HAP-Physicians é responsável pela gestão de médicos e consultas no sistema hospitalar. Este módulo funciona como um microserviço independente que permite o CRUD completo de médicos, agendamento de consultas e gestão de departamentos e especialidades médicas.

## Tecnologias Utilizadas

- Spring Boot 3.5.6
- Spring Data JPA
- Spring Security
- H2 Database (in-memory)
- Lombok
- Maven

## Estrutura do Projeto

```
src/main/java/leti_sisdis_6/happhysicians/
├── api/                           # Controllers REST
│   ├── AppointmentController.java
│   ├── PhysicianController.java
│   └── PhysicianMapper.java
├── config/                        # Configurações
│   ├── DataInitializer.java
│   └── SecurityConfig.java
├── dto/                          # Data Transfer Objects
│   ├── input/                    # DTOs de entrada
│   └── output/                   # DTOs de saída
├── exceptions/                   # Exceções personalizadas
│   ├── ConflictException.java
│   ├── NotFoundException.java
│   ├── GlobalExceptionHandler.java
│   └── ...
├── model/                        # Entidades JPA
│   ├── Physician.java
│   ├── Appointment.java
│   ├── Department.java
│   ├── Specialty.java
│   ├── AppointmentStatus.java
│   └── ConsultationType.java
├── repository/                   # Repositórios JPA
│   ├── PhysicianRepository.java
│   ├── AppointmentRepository.java
│   ├── DepartmentRepository.java
│   └── SpecialtyRepository.java
├── services/                     # Lógica de negócio
│   ├── PhysicianService.java
│   └── AppointmentService.java
└── util/                         # Utilitários
    ├── SlotCalculator.java
    └── AppointmentTimeValidator.java
```

## Configuração

### Porta da Aplicação
```
http://localhost:8081
```

### Base de Dados
- **Tipo**: H2 In-Memory
- **URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`
- **Password**: `password`
- **Console**: `http://localhost:8081/h2-console`

### Segurança
- Configuração básica para desenvolvimento
- Permitir todas as requisições (permitAll)
- CSRF desabilitado
- H2 console acessível

## Endpoints Principais

### Physicians
- `GET /physicians` - Listar todos os médicos
- `GET /physicians/{id}` - Obter médico por ID
- `POST /physicians` - Criar novo médico
- `PUT /physicians/{id}` - Atualizar médico
- `DELETE /physicians/{id}` - Eliminar médico
- `GET /physicians/specialty/{specialtyId}` - Médicos por especialidade
- `GET /physicians/department/{departmentId}` - Médicos por departamento

### Appointments
- `GET /appointments` - Listar todas as consultas
- `GET /appointments/{id}` - Obter consulta por ID
- `POST /appointments` - Criar nova consulta
- `PUT /appointments/{id}` - Atualizar consulta
- `DELETE /appointments/{id}` - Eliminar consulta
- `GET /appointments/physician/{physicianId}` - Consultas por médico
- `GET /appointments/patient/{patientId}` - Consultas por paciente

## Integração com Outros Módulos

Este módulo é independente e comunica com outros módulos via HTTP/REST:

- **hap-auth**: Autenticação e autorização
- **hap-patients**: Gestão de pacientes
- **hap-appointmentrecords**: Registos de consultas


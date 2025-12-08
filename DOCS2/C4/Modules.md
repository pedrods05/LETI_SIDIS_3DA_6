# C4 — Modules Overview (v2 - CQRS & Event Sourcing)

Este documento resume cada módulo do sistema HAP (versão atualizada com CQRS, Event Sourcing e Event-Driven Architecture) para facilitar a revisão do docente.

## Módulos
- hap-physicians
- hap-patients
- hap-auth
- hap-appointmentrecords

## Perfis/Portas por Instância
- **Physicians**: instance1 → 8081, instance2 → 8087
- **Patients**: instance1 → 8082, instance2 → 8088
- **Auth**: instance1 → 8084, instance2 → 8089
- **Appointment Records**: instance1 → 8083, instance2 → 8090

## Infraestrutura Partilhada
- **RabbitMQ**: Porta 5672 (AMQP), 15672 (Management UI)
  - Exchange: `hap-exchange` (topic)
  - Usado para comunicação assíncrona entre serviços e CQRS

---

## hap-physicians

### Overview
Responsável pelo ciclo de vida das consultas futuras (criação, atualização, cancelamento) e pela agregação de dados. Implementa **CQRS (Command Query Responsibility Segregation)** e **Event Sourcing** para audit trail completo. Suporta peer-forwarding entre instâncias para alta disponibilidade.

### Arquitetura
- **CQRS**: Separação entre Command (escrita) e Query (leitura)
  - **Write Model**: H2 (SQL) - para comandos e transações
  - **Read Model**: MongoDB - para queries otimizadas
- **Event Sourcing**: Event Store (H2) para histórico completo de eventos
- **Event-Driven**: Publica eventos via RabbitMQ para sincronização assíncrona

### Tecnologias Utilizadas
- Spring Boot 3.5.6, Spring Web, Spring Data JPA, Spring Security, Validation
- **H2** (Write DB + Event Store)
- **MongoDB** (Read DB - CQRS)
- **RabbitMQ** (Spring AMQP) - Event messaging
- Lombok, Maven

### Comunicação Inter-Serviços

#### HTTP/REST (Síncrono)
- **hap-patients**: Obter dados de paciente (GET /patients/{id})
- **hap-appointmentrecords**: Consultar registos concluídos (GET /api/appointment-records/{id})
- **hap-auth**: Validação de credenciais e autenticação com peers

#### AMQP (Assíncrono - RabbitMQ)
- **Publica eventos**:
  - `appointment.created` → AppointmentCreatedEvent
  - `appointment.updated` → AppointmentUpdatedEvent
  - `appointment.canceled` → AppointmentCanceledEvent
  - `appointment.reminder` → AppointmentReminderEvent
  - `physician.registered` → PhysicianRegisteredEvent
  - `physician.updated` → PhysicianUpdatedEvent
- **Consome eventos**: Atualiza read model (MongoDB) via Event Handlers

### Estrutura do Projeto
```
api/              # Controllers (REST endpoints) + Mappers
command/          # Command Services (CQRS - Write side)
query/            # Query Services (CQRS - Read side) + Repositories (MongoDB)
services/         # Domain Services
repository/       # Write Repositories (JPA/H2)
eventsourcing/    # Event Store (Event Sourcing)
events/           # Event Handlers (RabbitMQ listeners) + Events
util/             # Utilities (validators, calculators)
http/             # HTTP clients (ResilientRestTemplate)
config/           # Configuration (RabbitMQ, Security, OpenAPI, etc.)
model/            # Domain entities
dto/              # Data Transfer Objects
  input/          # Input DTOs
  output/         # Output DTOs
  request/        # Request DTOs
  response/       # Response DTOs
exceptions/       # Custom exceptions + GlobalExceptionHandler
setup/            # Data bootstrap/initialization
```

### Endpoints Principais

#### Gestão de Médicos
- `POST /physicians/register` - Registrar médico (via Command Service)
- `GET /physicians/{id}` - Consultar médico (via Query Service - MongoDB)
- `PUT /physicians/{id}` - Atualizar médico (via Command Service)
- `GET /physicians/{id}/slots` - Calcular slots disponíveis

#### Gestão de Consultas
- `POST /appointments` - Criar consulta (via Command Service)
- `GET /appointments` - Listar consultas (via Query Service - MongoDB)
- `GET /appointments/{id}` - Consultar consulta (via Query Service)
- `PUT /appointments/{id}` - Atualizar consulta (via Command Service)
- `PUT /appointments/{id}/cancel` - Cancelar consulta (via Command Service)
- `GET /appointments/upcoming` - Listar consultas futuras (via Query Service)
- `GET /appointments/{id}/audit-trail` - **NOVO**: Obter histórico completo (Event Sourcing)

#### Endpoints Internos
- `GET /internal/physicians/{id}` - Peer forwarding
- `GET /internal/appointments/{id}` - Peer forwarding

### Funcionalidades Principais

#### CQRS (Command Query Responsibility Segregation)
- **Command Side**: 
  - `AppointmentCommandService` - Processa comandos (create, update, cancel)
  - `PhysicianCommandService` - Processa comandos de médicos
  - Persiste no Write Model (H2)
  - Publica eventos via RabbitMQ
  
- **Query Side**:
  - `AppointmentQueryService` - Otimizado para leitura
  - `PhysicianQueryService` - Otimizado para leitura
  - Lê do Read Model (MongoDB)
  - Fallback para Write Model se necessário

#### Event Sourcing
- **EventStore**: Armazena todos os eventos de consultas (append-only)
- **EventStoreService**: Gerencia eventos e histórico
- **Tipos de Eventos**:
  - `CONSULTATION_SCHEDULED`
  - `CONSULTATION_UPDATED`
  - `CONSULTATION_CANCELED`
  - `CONSULTATION_COMPLETED`
  - `CONSULTATION_RESCHEDULED`
  - `NOTE_ADDED`
- **Audit Trail**: Endpoint `/appointments/{id}/audit-trail` retorna histórico completo

#### Event Handlers
- `AppointmentEventHandler`: Atualiza MongoDB quando recebe eventos
- `PhysicianEventHandler`: Atualiza MongoDB quando recebe eventos
- `AppointmentReminderHandler`: Processa lembretes (simulação de email/SMS)

#### Peer Forwarding
- Resiliência: Se recurso não existe localmente, consulta peers
- Alta disponibilidade: Distribuição de carga entre instâncias

### Tratamento de Erros
- Exceções traduzidas em ProblemDetails/HTTP adequados
- Falhas remotas geram mensagens claras
- Circuit Breaker para comunicação externa (Resilience4j)

### Execução
```cmd
# Terminal 1 - Instance 1
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1

# Terminal 2 - Instance 2
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance2
```

---

## hap-patients

### Overview
Gestão de pacientes: registo e consulta de dados de paciente. Implementa **CQRS** com MongoDB para read model. Suporta peer-forwarding entre instâncias para leitura distribuída. Publica eventos quando pacientes são registados.

### Arquitetura
- **CQRS**: Separação entre Write (H2) e Read (MongoDB)
- **Event-Driven**: Publica `PatientRegisteredEvent` via RabbitMQ

### Tecnologias Utilizadas
- Spring Boot 3.5.6, Spring Web, Spring Data JPA, Spring Security, Validation
- **H2** (Write DB)
- **MongoDB** (Read DB - CQRS)
- **RabbitMQ** (Spring AMQP) - Event publishing
- Lombok, Maven

### Comunicação Inter-Serviços

#### HTTP/REST (Síncrono)
- **hap-auth**: 
  - Registo público (POST /api/public/register)
  - Validação de token (Authorization header)

#### AMQP (Assíncrono - RabbitMQ)
- **Publica eventos**:
  - `patient.registered` → PatientRegisteredEvent
- **Consome eventos**: Event Handler atualiza MongoDB read model

### Estrutura do Projeto
```
api/              # Controllers + Mappers
service/          # Services (Command + Query)
repository/       # Write Repositories (JPA/H2)
query/            # Query Repositories (MongoDB) + Summary
event/            # Event Handlers + Events
http/             # HTTP clients (ResilientRestTemplate)
config/           # Configuration (RabbitMQ, Security, OpenAPI, etc.)
model/            # Domain entities
dto/              # Data Transfer Objects
exceptions/       # Custom exceptions + GlobalExceptionHandler
setup/            # Data bootstrap/initialization
```

### Endpoints Principais
- `GET /patients/{id}` - Consultar paciente (via Query Service - MongoDB)
- `POST /api/v2/patients/register` - Registar paciente (via Command Service)
- `PUT /patients/{id}` - Atualizar paciente
- `GET /internal/patients/{id}` - Peer forwarding

### Funcionalidades Principais

#### CQRS
- **Command Side**: `PatientRegistrationService` - Processa registos
- **Query Side**: `PatientQueryService` - Otimizado para leitura (MongoDB)

#### Event Handler
- `PatientEventHandler`: Atualiza MongoDB quando recebe `PatientRegisteredEvent`

### Execução
```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1
```

---

## hap-auth

### Overview
Autenticação e registo público. Emite/valida tokens JWT. Suporta autenticação distribuída com peer-forwarding entre instâncias.

### Tecnologias Utilizadas
- Spring Boot 3.5.6, Spring Web, Spring Security, Validation
- **H2** (In-memory ou persistente)
- JWT (Spring Security OAuth2)
- Lombok, Maven

### Comunicação Inter-Serviços
- Normalmente é **chamado** pelos outros serviços (não chama outros)
- Suporta peer-forwarding para autenticação distribuída

### Estrutura do Projeto
```
api/                          # Controllers (AuthApi, InternalAuthApi, AuthHelper)
services/                     # Services (AuthService)
usermanagement/               # User management
  model/                      # User, Role entities
  repository/                 # UserRepository, UserInMemoryRepository
configuration/                # Security configuration, JacksonConfig
dto/                          # Data Transfer Objects
exceptions/                   # Custom exceptions
setup/                        # Admin bootstrap/initialization
```

### Endpoints Principais
- `POST /api/public/login` - Login e obtenção de token JWT
- `POST /api/public/register` - Registo público
- `GET /api/internal/users/{id}` - Consulta interna (peer forwarding)
- `GET /api/internal/users/by-username/{username}` - Consulta por username
- `POST /api/internal/auth/authenticate` - Autenticação interna

### Funcionalidades Principais
- Gestão de credenciais básicas
- Emissão/validação de tokens JWT
- Peer forwarding para autenticação distribuída
- Roles: ADMIN, PATIENT, PHYSICIAN

### Execução
```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1
```

---

## hap-appointmentrecords

### Overview
Gestão de registos de consultas concluídas (diagnóstico, recomendações, prescrições, duração). Fonte de verdade para records. Implementa **CQRS** com MongoDB para read model. Consome eventos de appointments para manter sincronização.

### Arquitetura
- **CQRS**: Separação entre Write (H2) e Read (MongoDB)
- **Event-Driven**: Consome eventos de appointments via RabbitMQ

### Tecnologias Utilizadas
- Spring Boot 3.5.6, Spring Web, Spring Data JPA, Spring Security, Validation
- **H2** (Write DB)
- **MongoDB** (Read DB - CQRS)
- **RabbitMQ** (Spring AMQP) - Event consumption
- Lombok, Maven

### Comunicação Inter-Serviços

#### HTTP/REST (Síncrono)
- **hap-physicians**: Obter dados da consulta (GET /appointments/{id})
- **hap-patients**: Obter dados do paciente (GET /patients/{id})
- **hap-auth**: Validação de token (Authorization)

#### AMQP (Assíncrono - RabbitMQ)
- **Consome eventos**:
  - `appointment.created` → Sincroniza dados
  - `appointment.updated` → Atualiza projeções
  - `appointment.canceled` → Atualiza estado

### Estrutura do Projeto
```
api/              # Controllers (AppointmentRecordController, PeerHealthController)
service/          # Services
  event/          # Event Listeners/Publishers + Events
repository/       # Write Repositories (JPA/H2) + Projection Repositories (MongoDB)
http/             # HTTP clients (ExternalServiceClient)
config/           # Configuration (MongoDB, RabbitMQ, Security, CorrelationIdFilter)
model/            # Domain entities + Projections
dto/              # Data Transfer Objects
  input/          # Input DTOs
  output/         # Output DTOs
  local/          # Local DTOs
exceptions/       # Custom exceptions + GlobalExceptionHandler
setup/            # Data bootstrap/initialization
```

### Endpoints Principais
- `POST /api/appointment-records/{id}/record` - Criar registo de consulta
- `GET /api/appointment-records/{id}` - Consultar registo
- `GET /api/appointment-records/patient/{patientId}` - Registos do paciente
- `GET /api/peers/health` - **NOVO**: Health check de peers

### Funcionalidades Principais

#### CQRS
- **Command Side**: `AppointmentRecordService` - Cria registos
- **Query Side**: Usa projeções MongoDB para leitura otimizada

#### Event Listeners
- `AppointmentEventsListener`: Consome eventos de appointments
- Mantém projeções sincronizadas no MongoDB

### Execução
```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1
```

---

## Padrões Arquiteturais Implementados

### 1. CQRS (Command Query Responsibility Segregation)
- **Separação de responsabilidades**: Comandos (escrita) vs Queries (leitura)
- **Write Model**: H2 (SQL) - Transacional, ACID
- **Read Model**: MongoDB - Otimizado para queries, eventualmente consistente
- **Benefícios**: Escalabilidade, performance de leitura, flexibilidade

### 2. Event Sourcing
- **Event Store**: Armazena todos os eventos (append-only)
- **Audit Trail**: Histórico completo de mudanças
- **Reconstrução**: Estado pode ser reconstruído a partir de eventos
- **Implementado em**: hap-physicians (appointments)

### 3. Event-Driven Architecture
- **RabbitMQ**: Message broker para eventos assíncronos
- **Event Handlers**: Processam eventos e atualizam read models
- **Desacoplamento**: Serviços comunicam via eventos, não diretamente
- **Benefícios**: Escalabilidade, resiliência, flexibilidade

### 4. Polyglot Persistence
- **H2**: Write models (transacional)
- **MongoDB**: Read models (otimizado para queries)
- **Cada serviço escolhe a melhor tecnologia para seu caso**

### 5. Peer Forwarding
- **Alta Disponibilidade**: Múltiplas instâncias do mesmo serviço
- **Resiliência**: Se recurso não existe localmente, consulta peers
- **Distribuição de Carga**: Requests distribuídos entre instâncias


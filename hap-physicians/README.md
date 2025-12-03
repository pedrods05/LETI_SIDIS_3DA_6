# hap-physicians — Gestão de Consultas (Futuras)

Este serviço gere consultas futuras (criação, atualização, cancelamento), agrega dados de paciente quando necessário e suporta peer-forwarding entre instâncias.

## Perfis e Portas
- instance1 → 8081
- instance2 → 8087

## Executar (Windows, cmd.exe)
```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1
```
Para a segunda instância:
```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance2
```

## Endpoints principais
- POST /physicians/register
- POST /appointments
- GET  /physicians/{id}
- GET  /appointments
- PUT  /appointments/{id}
- PUT  /appointments/{id}/cancel
- GET  /appointments/upcoming

## Arquitetura e Design - Domain-Driven Design (DDD)

### Evolução: Monolito → Microserviços

O microserviço `hap-physicians` foi concebido através da decomposição de uma arquitetura monolítica hipotética, aplicando princípios de **Domain-Driven Design (DDD)**.

#### Bounded Context: "Physician & Appointment Management"

O `hap-physicians` representa um **Bounded Context** focado na gestão de médicos e agendamento de consultas futuras:

- **Gestão de Médicos**: Registro, consulta e manutenção de informações de médicos
- **Agendamento de Consultas**: Criação, atualização, cancelamento e consulta de consultas futuras
- **Relacionamentos**: Associação entre médicos e consultas, validação de disponibilidade

#### Agregados

1. **Agregado `Physician`** (Aggregate Root)
   - Entidade Raiz: `Physician`
   - Relacionamentos: `Department`, `Specialty`
   - Regras: Licença única, associação obrigatória a especialidade/departamento

2. **Agregado `Appointment`** (Aggregate Root)
   - Entidade Raiz: `Appointment`
   - Referências: `patientId` (externo), `Physician` (interno)
   - Regras: Validação de conflitos, disponibilidade, estados (SCHEDULED, CANCELED, COMPLETED)

#### Justificativa do Microserviço

O `hap-physicians` existe como microserviço independente porque:

1. **Responsabilidade Única**: Foco exclusivo em médicos e agendamentos futuros
2. **Ciclo de Vida Independente**: Desenvolvimento, teste e deploy independentes
3. **Escalabilidade Específica**: Escalável conforme demanda de agendamentos
4. **Tecnologias Específicas**: Permite CQRS com MongoDB para read models
5. **Bounded Context Claro**: Domínio bem definido com linguagem ubíqua

**Limites**:
- **Dentro do escopo**: Registro de médicos, gestão de consultas futuras, validações
- **Fora do escopo**: Gestão de pacientes (`hap-patients`), registros médicos (`hap-appointmentrecords`), autenticação (`hap-auth`)

## CQRS

O padrão **Command-Query Responsibility Segregation (CQRS)** separa operações de leitura (queries) das operações de escrita (commands).

### Commands (Escrita)

Operações que modificam o estado, implementadas em `PhysicianCommandService` e `AppointmentCommandService`:

- `POST /physicians/register` → Salva no Write Model (H2) e publica `PhysicianRegisteredEvent`
- `POST /appointments` → Salva no Write Model e publica `AppointmentCreatedEvent`
- `PUT /appointments/{id}` → Atualiza e publica `AppointmentUpdatedEvent`
- `PUT /appointments/{id}/cancel` → Cancela e publica `AppointmentCanceledEvent`

Todos os commands são **transacionais** e garantem consistência imediata no Write Model (H2/JPA).

### Queries (Leitura)

Operações que apenas consultam dados, implementadas em `PhysicianQueryService` e `AppointmentQueryService`:

- `GET /physicians/{id}` → Consulta Read Model (MongoDB) com fallback para Write Model
- `GET /appointments` → Consulta Read Model
- `GET /appointments/upcoming` → Query específica otimizada

As queries consultam preferencialmente o **Read Model (MongoDB)**, otimizado para leitura rápida.

### Separação de Modelos

- **Write Model (H2/JPA)**: Banco relacional para escrita
  - Garante integridade referencial e consistência transacional
  - Modelos: `Physician`, `Appointment` (entidades JPA)
  
- **Read Model (MongoDB)**: Banco NoSQL para leitura
  - Desnormalizado para performance
  - Modelos: `PhysicianSummary`, `AppointmentSummary` (documentos MongoDB)
  - Atualizado assincronamente via eventos

### Eventos e Sincronização

Após cada escrita bem-sucedida, um evento é publicado via RabbitMQ:

| Evento | Routing Key | Handler | Ação |
|--------|-------------|---------|------|
| `PhysicianRegisteredEvent` | `physician.registered` | `PhysicianEventHandler` | Atualiza `PhysicianSummary` no MongoDB |
| `AppointmentCreatedEvent` | `appointment.created` | `AppointmentEventHandler` | Atualiza `AppointmentSummary` no MongoDB |
| `AppointmentUpdatedEvent` | `appointment.updated` | `AppointmentEventHandler` | Atualiza `AppointmentSummary` no MongoDB |
| `AppointmentCanceledEvent` | `appointment.canceled` | `AppointmentEventHandler` | Atualiza status para "CANCELED" no MongoDB |

**Consistência Eventual**: O Read Model é atualizado de forma assíncrona. O mecanismo de fallback garante que queries sempre retornem dados, mesmo que o Read Model ainda não tenha sido atualizado.

### Exemplo Prático: Registrar um Médico

Este exemplo demonstra o fluxo completo de uma operação CQRS, desde a requisição HTTP até a atualização do Read Model.

#### Requisição Inicial

**Cliente faz requisição**:
```http
POST /physicians/register
Content-Type: application/json

{
  "fullName": "Dr. João Silva",
  "licenseNumber": "MED12345",
  "username": "joao.silva@hospital.com",
  "specialtyId": "SPEC001",
  "departmentId": "DEPT001",
  ...
}
```

#### Fluxo Completo (Passo a Passo)

**1. Controller recebe requisição**
- `PhysicianController.registerPhysician()` recebe o `RegisterPhysicianRequest`
- Delega para `PhysicianCommandService.registerPhysician()`

**2. Command Service processa (Write Side)**
- `PhysicianCommandService` chama `PhysicianService.register()`
- Validações: verifica se username e licenseNumber já existem
- Cria usuário de autenticação via `hap-auth` (HTTP REST síncrono)
- Salva `Physician` no **Write Model (H2)** dentro de uma transação
- Retorna `PhysicianIdResponse` com o ID gerado

**3. Publicação de Evento**
- Após sucesso da transação, `PhysicianCommandService` publica evento:
  ```java
  rabbitTemplate.convertAndSend("hap-exchange", "physician.registered", event);
  ```
- Evento `PhysicianRegisteredEvent` contém: `physicianId`, `fullName`, `licenseNumber`, `username`, `specialtyId`, `specialtyName`, `departmentId`, `departmentName`
- **Resposta HTTP é enviada ao cliente** (não espera processamento do evento)

**4. RabbitMQ processa evento**
- Exchange `hap-exchange` (Topic) recebe o evento
- Roteia para queue `q.physician.summary.updater` baseado na routing key `physician.registered`
- Evento fica na queue aguardando consumo

**5. Event Handler consome (Read Side)**
- `PhysicianEventHandler.handlePhysicianRegistered()` é acionado automaticamente
- Cria `PhysicianSummary` a partir dos dados do evento
- Salva no **Read Model (MongoDB)** na collection `physician_summaries`
- Log: `✅ [Query Side] Guardado no MongoDB: {physicianId}`

#### Resultado Final

**Write Model (H2)**:
- Entidade `Physician` completa salva com todos os relacionamentos
- Dados transacionais e normalizados
- Disponível imediatamente após a transação

**Read Model (MongoDB)**:
- Documento `PhysicianSummary` salvo na collection `physician_summaries`
- Dados desnormalizados otimizados para leitura
- Disponível após processamento assíncrono do evento (alguns milissegundos depois)

#### Consulta Posterior (Query)

Quando um cliente consulta o médico:

```http
GET /physicians/{physicianId}
```

**Fluxo de Query**:
1. `PhysicianController.getPhysician()` delega para `PhysicianQueryService.getPhysicianById()`
2. `PhysicianQueryService` consulta primeiro o **Read Model (MongoDB)**
3. Se encontrado, retorna dados do `PhysicianSummary`
4. Se não encontrado ou incompleto, faz **fallback** para **Write Model (H2)**
5. Retorna dados enriquecidos ao cliente

#### Pontos Importantes

- **Tempo de resposta**: Cliente recebe resposta imediata (não espera atualização do Read Model)
- **Consistência eventual**: Read Model é atualizado assincronamente (normalmente em milissegundos)
- **Fallback**: Se Read Model não estiver atualizado, consulta Write Model
- **Desacoplamento**: Write Side não conhece Read Side diretamente (comunicação via eventos)

## Assignment 2 - Implementações

### Visão Geral

Implementações realizadas no Assignment 2:

1. **CQRS**: Separação de operações de leitura e escrita
2. **AMQP/RabbitMQ**: Comunicação assíncrona via mensageria
3. **Database-per-Service**: Separação de bancos por responsabilidade
4. **Múltiplas Instâncias**: Deploy de duas instâncias (8081, 8087)
5. **HTTP REST**: Manutenção de endpoints REST para integração

### AMQP e Message Broker (RabbitMQ)

**Exchange**: `hap-exchange` (Topic Exchange)  
**Configuração**: `RabbitMQConfig.java`  
**Message Converter**: `Jackson2JsonMessageConverter` (JSON)

**Queues criadas automaticamente**:
- `q.physician.summary.updater` → Consumida por `PhysicianEventHandler`
- `q.appointment.summary.updater` → Consumida por `AppointmentEventHandler`

**Fluxo de Eventos**:
```
Command Service → Write Model (H2) → Publica Evento → RabbitMQ → Queue → Event Handler → Read Model (MongoDB)
```

### Database-per-Service Pattern

Implementamos **Database-per-Responsibility** (variação do Database-per-Service):

**Write Model (H2 - JPA)**
- Tecnologia: H2 in-memory (desenvolvimento)
- Uso: Operações de escrita (Commands)
- Características: ACID, integridade referencial, consistência imediata
- **Por instância**: Cada instância tem seu próprio banco H2 (isolamento de transações)

**Read Model (MongoDB)**
- Tecnologia: MongoDB (NoSQL)
- Uso: Operações de leitura (Queries)
- Características: Desnormalizado, consultas rápidas, escalável
- **Compartilhado**: Ambas as instâncias conectam ao mesmo database `happhysicians_db` (consistência de leitura)

**Estratégia de Bancos de Dados**:
- **H2 Separado**: Isolamento de dados de escrita, evita conflitos, permite processamento paralelo
- **MongoDB Compartilhado**: Consistência de leitura entre instâncias, read model representa estado agregado
- **O que acontece ao remover instância**: 
  - H2: Dados perdidos (in-memory), mas eventos já publicados estão no RabbitMQ
  - MongoDB: Dados preservados (compartilhado)
  - Eventos: Preservados no RabbitMQ, podem ser processados por outras instâncias

### Múltiplas Instâncias

**Configuração**:
- Instance 1: Porta 8081, Profile `instance1`
- Instance 2: Porta 8087, Profile `instance2`

**Compartilhamento de Recursos**:
- **RabbitMQ**: Mesmo exchange e queues (event-driven)
- **MongoDB**: Mesmo database para Read Model
- **H2**: Banco separado por instância (isolamento de escrita)

**Peer-forwarding**: Se um recurso não existir na instância local, o serviço tenta os peers usando endpoints públicos.

### HTTP REST para Comunicação Externa

**ExternalServiceClient** gerencia comunicações HTTP:

- **hap-patients**: `GET /patients/{id}` ou `/internal/patients/{id}` (enriquecimento de dados)
- **hap-appointmentrecords**: `GET /api/appointment-records/{id}` (validação)
- **hap-auth**: `POST /auth/users` (criação de usuários)

**Propagação de Headers**: `Authorization`, `X-User-Id`, `X-User-Role` são propagados automaticamente.

**Estratégia de Fallback**: Tenta primeiro endpoint interno `/internal/patients/{id}`, depois público `/patients/{id}`.

### Análise: Síncrono vs Assíncrono

**Síncrono (HTTP REST)** - Usado para:
- Validações críticas em tempo real (ex: verificar se paciente existe)
- Operações transacionais (ex: criar usuário de autenticação)
- Enriquecimento de dados para resposta imediata

**Assíncrono (RabbitMQ)** - Usado para:
- Atualização de Read Models (CQRS)
- Desacoplamento de componentes
- Processamento em background

**Matriz de Decisão**:

| Critério | Síncrono (REST) | Assíncrono (RabbitMQ) |
|----------|-----------------|----------------------|
| Resposta Imediata | ✅ | ❌ |
| Consistência Transacional | ✅ | ❌ (Eventual) |
| Validação Crítica | ✅ | ❌ |
| Atualização Read Model | ❌ | ✅ |
| Desacoplamento | ❌ | ✅ |

## Configuração e Execução

### Pré-requisitos

1. **RabbitMQ**: `localhost:5672` (guest/guest), Management UI: `http://localhost:15672`
2. **MongoDB**: `localhost:27017`, Database: `happhysicians_db`, User: `root`, Password: `secretpassword`

### Iniciar Serviços

```bash
# Docker Compose 
docker compose up -d

# Ou manualmente
docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:management
docker run -d -p 27017:27017 -e MONGO_INITDB_ROOT_USERNAME=root -e MONGO_INITDB_ROOT_PASSWORD=secretpassword mongo
```

### Executar Instâncias

```cmd
# Terminal 1 - Instance 1
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1

# Terminal 2 - Instance 2
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance2
```

### Verificar Funcionamento

1. **Logs**: Procurar por `✅ Exchange 'hap-exchange' declarado`, `📥 [Query Side] Recebi evento`
2. **RabbitMQ UI**: `http://localhost:15672` - Verificar exchange e queues
3. **MongoDB**: Verificar collections `physician_summaries`, `appointment_summaries`
4. **Swagger**: `http://localhost:8081/swagger-ui.html` - Testar endpoints

## Testes de Integração

Testes disponíveis no **Hoppscotch** cobrindo:

1. **CQRS - Commands**: Verificar escrita no Write Model e publicação de eventos
2. **CQRS - Queries**: Verificar leitura do Read Model (MongoDB)
3. **Eventos**: Verificar sincronização após commands
4. **Múltiplas Instâncias**: Testar peer-forwarding e consistência
5. **Comunicação Externa**: Verificar chamadas HTTP e propagação de headers

**Como usar**: Importar coleção SIDIS no Hoppscotch, configurar variáveis de ambiente, executar testes na ordem sugerida.

## Estrutura de Código

```
hap-physicians/
├── command/          # Command Services (escrita)
├── query/            # Query Services e Read Models (MongoDB)
├── events/           # Eventos e Event Handlers
├── config/           # Configurações (RabbitMQ, MongoDB)
├── repository/       # Repositories do Write Model (JPA)
└── model/            # Entidades do Write Model (JPA)
```

## Colaboração entre serviços (HTTP/REST)
- Patients: `GET http://localhost:{8082|8088}/patients/{id}`
- Appointment Records: `GET http://localhost:{8083|8090}/api/appointment-records/{id}`
- Auth: Propagação de `Authorization`, `X-User-Id`, `X-User-Role`

## Swagger
- Instance 1: `http://localhost:8081/swagger-ui.html`
- Instance 2: `http://localhost:8087/swagger-ui.html`

## Limitações conhecidas
- Sem service discovery (peers configurados manualmente)
- Sem circuit breaker/retries com backoff
- Sem cache distribuída
- H2 in-memory (dados perdidos ao reiniciar)

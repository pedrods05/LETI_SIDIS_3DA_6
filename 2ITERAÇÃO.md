# HAP Platform - Architecture & Design Decisions (Assignment 2)

Este documento regista as principais decisões arquiteturais tomadas durante a migração da plataforma HAP para uma arquitetura distribuída e orientada a eventos.

---

## 1. Padrão Arquitetural: CQRS (Command Query Responsibility Segregation)
Write Side (Commands): Responsável pela validação de regras de negócio e persistência transacional
- POST /api/public/register → Command: RegisterUser
- POST /api/public/login → Command: AuthenticateUser
- POST /api/v2/patients/register → Command: RegisterPatient
- POST /physicians/register → Command: RegisterPhysician
- POST /appointments → Command: CreateAppointment
- PUT /appointments/{id} → Command: UpdateAppointment
- PUT /appointments/{id}/cancel → Command: CancelAppointment
- POST /api/appointment-records/{id}/record → Command: CreateAppointmentRecord
Read Side (Queries): Responsável por servir dados rapidamente ao cliente, utilizando projeções de dados desnormalizadas.
- GET /patients/{id} → Query: GetPatientById
- GET /physicians/{id} → Query: GetPhysicianById
- GET /appointments → Query: GetAllAppointments
- GET /appointments/upcoming → Query: GetUpcomingAppointments
- GET /api/appointment-records/{id} → Query: GetAppointmentRecordById

## 5. Análise dos Modelos de Comunicação: Estratégia Híbrida

Para cumprir os requisitos de resiliência e usabilidade, adotámos uma abordagem híbrida que combina comunicação síncrona (REST) e assíncrona (AMQP), aplicando cada uma onde traz mais valor arquitetural.

### 5.1. Modelo Síncrono (HTTP/REST)
**Onde é usado:** 1.  **Endpoints Externos (Edge API):** Toda a comunicação entre o cliente (Frontend/Postman) e os microserviços (ex: `POST /api/v2/patients/register`).
2.  **Dependências Críticas (Auth):** Comunicação entre serviços de negócio e o `hap-auth`.

**Justificação:**
* **Feedback Imediato:** O cliente necessita de confirmação instantânea do sucesso ou falha da validação dos dados (ex: erro 400 se o email for inválido).
* **Simplicidade:** O protocolo HTTP é universal e fácil de consumir por clientes web e mobile.
* **Consistência Estrita (Auth):** No caso do registo no `hap-auth`, optámos por manter a chamada síncrona (`RestTemplate`) porque a criação das credenciais de login é um pré-requisito obrigatório para a criação do perfil clínico. Se o Auth falhar, a transação deve abortar imediatamente.

### 5.2. Modelo Assíncrono (AMQP)
**Onde é usado:**
1.  **Propagação de Estado (CQRS):** Sincronização entre o *Write Model* (SQL) e o *Read Model* (MongoDB).
2.  **Integração entre Domínios:** Notificação de eventos de negócio (ex: `PatientRegistered`) para outros serviços interessados.

**Justificação:**
* **Desacoplamento Temporal:** O serviço `hap-patients` não precisa de conhecer quem consome os seus eventos, nem se esses serviços estão online no momento.
* **Latência Reduzida:** O tempo de resposta ao utilizador no `POST` é reduzido, pois não inclui o tempo de escrita no MongoDB ou processamentos secundários.
* **Resiliência:** Se o consumidor (Listener) estiver em baixo, a mensagem persiste na fila do RabbitMQ e será processada assim que o serviço recuperar, garantindo que nenhum dado é perdido (**At-Least-Once Delivery**).

### 5.3. Diagrama de Decisão de Comunicação

* **Cliente → Serviço:** REST (Bloqueante, espera resposta).
* **Serviço → Auth:** REST (Bloqueante, necessita confirmação de segurança).
* **Serviço → RabbitMQ:** AMQP (Não-bloqueante, "fire-and-forget").
* **RabbitMQ → Serviço (Outros/Query):** AMQP (Processamento em background).

Esta separação assegura que o sistema é responsivo para o utilizador, mas resiliente e escalável nos processos internos.
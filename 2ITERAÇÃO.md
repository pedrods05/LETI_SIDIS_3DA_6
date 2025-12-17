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

No nosso protótipo, o modelo de leitura em MongoDB é utilizado principalmente em hap-patients e hap-appointmentrecords para projeções específicas (por exemplo, resumos de pacientes ou registos de consultas). Algumas queries, como GET /appointments, continuam a ler diretamente do modelo relacional, mantendo ainda assim a separação lógica entre comandos e queries.
## 2. Database per service
- Cada microserviço (Auth, Patients, Physicians, AppointmentRecords) possui a sua própria base de dados de escrita (H2/relacional em dev, equivalente a PostgreSQL/SQL Server em produção) e, quando aplicável, a sua própria base de dados de leitura (MongoDB).
- Não existe acesso direto entre bases de dados; os serviços comunicam exclusivamente por HTTP/REST ou eventos AMQP (RabbitMQ).
- Nota sobre Persistência e Ciclo de Vida dos Dados:
- Ambiente de Desenvolvimento (Write Model): Utilizamos H2 em memória. Isto significa que a base de dados "vive" dentro da instância do microserviço.
- Consequência: Se a instância for desligada ou reiniciada, os dados relacionais perdem-se. Cada nova instância arranca com uma base de dados vazia (ou recriada pelo DataBootstrap). Não há partilha de dados entre instâncias do mesmo serviço (daí a necessidade do Peer-Forwarding ou Data Seeding).
- Ambiente de Desenvolvimento (Read Model): Utilizamos MongoDB em contentor Docker.
- Consequência: Os dados persistem em Volumes Docker mesmo que o contentor do microserviço Java seja destruído. Múltiplas instâncias do mesmo serviço (instance1, instance2) podem ligar-se ao mesmo contentor MongoDB partilhado, garantindo que ambas vêem as mesmas projeções de leitura.

## 3. Event Sourcing e Auditabilidade
Embora o sistema mantenha o estado atual nas tabelas relacionais (Snapshot) para operações do dia-a-dia, implementámos um padrão de **Event Sourcing** paralelo focado na auditabilidade e recuperação histórica.
- **Event Store:** Os eventos críticos de domínio (ex: `PatientRegistered`, `AppointmentCreated`, `ConsultationScheduled`) são persistidos numa registo imutável antes de serem publicados no *message broker*.
- **Benefício:** Isto garante uma "Single Source of Truth" histórica, permitindo no futuro reconstruir o estado do sistema (*Replay*) ou gerar novas projeções de dados (ex: relatórios de BI) sem perder informação passada.

## 4. API-Led Architecture (System / Process / Experience APIs)

Embora não tenhamos introduzido um API Gateway dedicado, a modelação das APIs segue princípios de API-Led Architecture, separando responsabilidades em três níveis lógicos:

**System APIs (dados crus / CRUD):**
- Exposição direta de recursos de cada microserviço, focada em operações básicas sobre as entidades de domínio.
- Exemplos:
  - `GET /patients/{id}`, `PATCH /patients/me` (hap-patients)
  - `GET /physicians/{id}`, `POST /physicians/register` (hap-physicians)
  - `GET /api/appointment-records/{id}`, `POST /api/appointment-records/{id}/record` (hap-appointmentrecords)
  - `POST /api/public/register`, `POST /api/public/login` (hap-auth)

**Process APIs (orquestração / fluxos de negócio):**
- Endpoints que coordenam múltiplos sistemas ou múltiplas operações internas, encapsulando regras de negócio.
- Exemplos:
  - `POST /appointments`, `PUT /appointments/{id}`, `PUT /appointments/{id}/cancel` em hap-physicians:
    - Validam o estado da consulta, verificam médico/paciente, comunicam com AppointmentRecords e publicam eventos.
  - `POST /api/v2/patients/register` em hap-patients:
    - Encapsula validação de consentimento, criação do perfil clínico e chamada síncrona ao hap-auth para criar credenciais.

**Experience APIs (respostas adaptadas à UI):**
- Endpoints e DTOs desenhados para fornecer dados já agregados e prontos para consumo pelo frontend, reduzindo lógica duplicada no cliente.
- Exemplos:
  - `GET /appointments/upcoming` (hap-physicians): devolve uma lista de consultas futuras já ordenadas e agregadas.
  - `GET /patients/{id}` / `GET /patients/{id}/profile` (hap-patients): devolvem `PatientProfileDTO` com informação consolidada do paciente.

Esta separação lógica permite:
- Reutilizar System APIs entre diferentes Process/Experience APIs sem duplicar lógica.
- Manter a orquestração de fluxos de negócio centralizada em poucos endpoints (Process APIs).
- Fornecer respostas específicas para o frontend (Experience APIs) sem expor diretamente o modelo interno das entidades.

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
### 5.4. RabbitMQ

O RabbitMQ atua como o barramento de eventos assíncrono da plataforma HAP. A sua principal função é desacoplar o processo de escrita (transacional/síncrono) dos processos de leitura e integração, garantindo que a experiência do utilizador permanece rápida e que o sistema é resiliente a falhas parciais.
* **Comunicação Assíncrona:** Desacoplamento temporal entre serviços (Fire-and-Forget).
* **Suporte ao CQRS:** Ponte de sincronização entre Base de Dados Relacional (Write) e MongoDB (Read).
* **Consistência Eventual:** Prioridade à disponibilidade e baixa latência na escrita.
- **Fluxo de dados:**
* **Produtor:** Serviço executa transação local (SQL) → Publica Evento
* **Broker:** RabbitMQ encaminha mensagem para as filas baseadas na Routing Key.
* **Consumidor:** Listener recebe JSON → Atualiza Projeção no MongoDB.

## 6. Gestão de Transações Distribuídas: Saga Pattern
Como as bases de dados estão isoladas por serviço, não podemos utilizar transações ACID globais. Adotámos o padrão **Saga baseada em Coreografia** para garantir a consistência eventual dos dados entre serviços.

- **Fluxo Descentralizado:** Um serviço publica um evento de domínio (ex: `AppointmentCanceled`) e os serviços interessados reagem a esse evento para atualizar o seu estado local, sem a necessidade de um orquestrador central.
- **Exemplo Prático (Cancelamento):**
  1. O Médico cancela a consulta no *Physicians Service*.
  2. O evento `AppointmentCanceled` é publicado no RabbitMQ.
  3. O *AppointmentRecords Service* consome o evento e atualiza o estado do registo clínico local para "CANCELLED".
- **Compensação:** O sistema foi desenhado para lidar com falhas através de ações de compensação ou retenção de mensagens (*Retry*) até que o serviço dependente esteja disponível.

## 7. Resiliência e Tolerância a Falhas
Para evitar falhas em cascata (*Cascading Failures*) quando um serviço está indisponível ou lento, implementámos padrões de resiliência robustos utilizando a biblioteca **Resilience4j**:

- **Circuit Breaker:** Protege o sistema impedindo chamadas síncronas repetidas a um serviço que já está em falha (ex: o `hap-appointmentrecords` a consultar o `hap-physicians`). Se a taxa de erro exceder um limiar configurado, o circuito "abre" e falha rapidamente ou devolve uma resposta de *fallback* pré-definida.
- **Retry com Backoff Exponencial:** Para falhas transientes de rede, o sistema tenta repetir a operação automaticamente um número limitado de vezes, com intervalos crescentes, antes de desistir.
- **Fallback P2P (Peer-to-Peer):** No serviço `hap-patients`, implementámos um mecanismo de alta disponibilidade onde, se a base de dados local falhar, o serviço tenta contactar outras instâncias do cluster (*peers*) para obter os dados do paciente, garantindo a disponibilidade (AP no teorema CAP) em detrimento da consistência imediata.

## 8. Observabilidade e Rastreamento Distribuído
Numa arquitetura de microserviços, depurar um pedido que atravessa múltiplos componentes é complexo. Para mitigar isto, implementámos uma estratégia de observabilidade completa:

- **Distributed Tracing (Zipkin):** Cada pedido HTTP ou mensagem AMQP é etiquetada com um `TraceID` e `SpanID`. Isto permite visualizar no Zipkin o percurso completo e a latência de uma transação (ex: tempo gasto no `hap-patients` vs tempo de espera na fila RabbitMQ).
- **Correlation IDs nos Logs:** Utilizamos `MDC` (*Mapped Diagnostic Context*) para injetar o ID de correlação em todos os logs da aplicação. Isto permite filtrar no terminal ou no ELK todos os logs relacionados com um único pedido de utilizador, independentemente do microserviço onde ocorreram.
- **Health Checks:** Exposição de endpoints `/actuator/health` que permitem ao orquestrador (Docker/Kubernetes) monitorizar o estado de *Liveness* e *Readiness* das instâncias e reiniciá-las se necessário.

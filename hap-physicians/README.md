# hap-physicians — Gestão de Médicos e Consultas

Este serviço gere o registo e a consulta de médicos, bem como o agendamento e gestão de consultas futuras. Suporta peer-forwarding entre instâncias para leitura distribuída e implementa CQRS com Event Sourcing para consultas.

## Perfis e Portas

- instance1 → 8081
- instance2 → 8087

## Executar (Windows, cmd.exe)

É necessário arrancar o RabbitMQ, MongoDB e Zipkin antes de iniciar a aplicação:

```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1
```
Para a segunda instância:
```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance2
```
## Endpoints principais

**Gestão de Médicos:**
- GET  /physicians/{id}
- POST /physicians/register
- PUT  /physicians/{id}
- GET  /physicians/{id}/slots

**Gestão de Consultas:**
- GET  /appointments/{id}
- POST /appointments
- PUT  /appointments/{id}
- PUT  /appointments/{id}/cancel
- GET  /appointments/upcoming
- GET  /appointments/{id}/audit-trail
- POST /appointments/{id}/notes

## Colaboração entre serviços (HTTP/REST)

- **hap-patients**: GET http://localhost:{8082|8088}/patients/{id} ou /internal/patients/{id}
- **hap-auth**: POST http://localhost:{8084|8089}/api/public/register, GET /auth/users/{id}
- **hap-appointmentrecords**: GET http://localhost:8083/api/appointment-records/{id}

- Propagação de headers (quando aplicável): Authorization, X-User-Id, X-User-Role

## Peer-forwarding

- Se um médico ou consulta não existir localmente, consulta peers pelas mesmas rotas públicas (evita endpoints internos para o cliente externo; os internos são usados apenas entre instâncias).
- A lista de peers é estática por perfil/instância (ex.: 8081 conhece 8087 e vice-versa) configurada via lista hardcoded em `ExternalServiceClient`.
- Se nenhuma instância tiver o recurso, o pedido devolve 404 (não há fallback "mágico").
- Implementado utilizando `ResilientRestTemplate` para lidar com falhas temporárias de peers.

## Configuração (exemplo)

- Bases URL remotas via application.properties (por profile):
  - hap.patients.base-url
  - hap.auth.base-url
  - hap.appointmentrecords.base-url

## Swagger
- http://localhost:8081/swagger-ui.html (instância 1)
- http://localhost:8087/swagger-ui.html (instância 2)

## Decisões e Notas
- **Separação leitura/escrita (CQRS):**
  - Lado comando (escrita) usa JPA + H2 via `PhysicianCommandService`/`AppointmentCommandService` e respetivos repositories.
  - Lado query (leitura) usa MongoDB via `PhysicianQueryService`/`AppointmentQueryService` e respetivos query repositories para respostas otimizadas.
- **Comunicação síncrona (HTTP/REST):**
  - Usada para interagir com o `hap-auth` (criar utilizadores ao registar médicos, validar tokens).
  - Usada para obter dados de pacientes do `hap-patients` (enriquecimento de dados nas consultas).
  - Usada para validar e criar registos médicos no `hap-appointmentrecords`.
  - Usada também entre containers quando é necessária consistência imediata (ex.: peer-forwarding para encontrar dados que ainda não foram replicados localmente).
- **Comunicação assíncrona (AMQP/RabbitMQ):**
  - Após operações de escrita bem-sucedidas, os command services publicam eventos no exchange `hap-exchange`:
    - `PhysicianRegisteredEvent`, `PhysicianUpdatedEvent`
    - `AppointmentCreatedEvent`, `AppointmentUpdatedEvent`, `AppointmentCanceledEvent`
    - `AppointmentReminderEvent`
  - `PhysicianEventHandler` e `AppointmentEventHandler` consomem esses eventos e atualizam o modelo de leitura em MongoDB.
  - Este padrão permite que outros serviços também reajam a eventos sem acoplamento forte.
- **Event log / Event Sourcing (para consultas):**
  - Para cada operação sobre uma consulta (criação, atualização, cancelamento, adição de notas), o serviço persiste um `EventStore` na tabela `event_store` com tipo de evento, `aggregateId` (appointmentId), metadados de auditoria e correlation ID.
  - O estado oficial da consulta continua na entidade JPA `Appointment`; o event log funciona como trilho de auditoria (audit trail) completo e permite reconstruir o histórico de eventos via `GET /appointments/{id}/audit-trail`.
  - Diferente do `hap-patients`, aqui o event store é usado para consultas (appointments), não para médicos (physicians).
- **Peer-forwarding HTTP entre instâncias:**
  - Continua ativo mesmo com CQRS/AMQP: se a instância local ainda não conhecer o médico ou consulta, tenta sequencialmente os peers configurados.
  - Implementado nos controllers (`PhysicianController`, `AppointmentController`) utilizando `ResilientRestTemplate` para lidar com falhas temporárias de peers.
- **Isolamento entre serviços:**
  - Não há imports diretos de classes de outros módulos; a integração é sempre via HTTP/REST ou eventos AMQP.
- **Circuit Breaker (Resilience4j):**
  - Implementado para chamadas ao `hap-auth` e `hap-appointmentrecords` via anotações `@CircuitBreaker` em `ExternalServiceClient`.
  - Configuração via `application.properties` com thresholds e timeouts específicos por serviço.
- **Sagas envolvendo médicos/consultas:**
  - Não existe uma Saga distribuída formal para o registo de médicos. A criação de credenciais em `hap-auth` é feita via chamada HTTP síncrona dentro de uma transação local.
  - Para consultas, a criação pode envolver validações síncronas (paciente existe, médico disponível) mas sem orquestração de Saga formal nem passos de compensação.
  - Os eventos publicados ficam disponíveis para que outros serviços possam reagir (coreografia leve), mas sem um orquestrador de Saga.

## Limitações conhecidas
- Service Discovery estático (via lista hardcoded de peers em `ExternalServiceClient`). Implementação de resiliência customizada (`ResilientRestTemplate`) para tolerância a falhas de rede entre instâncias, combinada com Circuit Breaker (Resilience4j) para serviços externos.
- Sem cache distribuída; consistência eventual entre instâncias.
- Eventos focados nos cenários principais (por exemplo, `PhysicianRegisteredEvent`, `AppointmentCreatedEvent`); extensões para outros eventos são possíveis mas não totalmente exploradas aqui.
- O módulo não aplica event sourcing completo para médicos: apenas para consultas (appointments). O estado oficial dos médicos está numa base relacional e não há event store para reconstruir o histórico de médicos.
- Não há Saga de registo de médico ou criação de consulta com vários passos assíncronos e compensações; optou-se por um fluxo mais simples (transação local + chamadas HTTP síncronas + eventos de integração).
- MongoDB read models são separados por instância (`happhysicians_db_1` e `happhysicians_db_2`), o que pode levar a inconsistências de leitura entre instâncias até que os eventos sejam processados.

## Testes e build
```cmd
mvnw.cmd -q test
mvnw.cmd -q -DskipTests package
```

## CQRS

- Na nossa implementação Java com Spring Boot, os conceitos de CQRS foram mapeados da seguinte forma:
- Os Commands (ex: RegisterPhysician, CreateAppointment) são representados pelos métodos transacionais nos `*CommandService`, que atuam sobre o modelo de escrita (JPA / base de dados relacional).
- As Queries (ex: GetPhysicianById, GetAppointmentById) são representadas pelos métodos de leitura nos `*QueryService`, que consultam o modelo de leitura (`*QueryRepository` / projeções de leitura em MongoDB).
- Os DTOs de entrada (por exemplo `RegisterPhysicianRequest`, `ScheduleAppointmentRequest`) funcionam como objetos de comando.

## Messaging e Tracing no hap-physicians

Este módulo usa RabbitMQ para publicar eventos sempre que um médico é registado/atualizado ou uma consulta é criada/atualizada/cancelada.
Os eventos são consumidos localmente por `PhysicianEventHandler` e `AppointmentEventHandler`, que atualizam o modelo de leitura em MongoDB (`PhysicianSummary`, `AppointmentSummary`).
Além dos logs, o sistema integra com o Zipkin (via Micrometer Tracing) para visualização gráfica das spans e latências. O X-Correlation-Id serve como TraceId, permitindo depurar o fluxo completo: REST Request -> RabbitMQ Publish -> RabbitMQ Consume -> MongoDB Write.

### Correlation IDs (Tracing de ponta a ponta)

Para permitir rastreio de um pedido entre serviços:

- Os controladores (`PhysicianController`, `AppointmentController`) aceitam opcionalmente o header HTTP `X-Correlation-Id`.
    - Se não existir, geram um UUID e colocam-no no MDC (contexto de logging) sob a mesma chave.
- O `RabbitTemplate` é configurado em `RabbitMQConfig` com um `beforePublishPostProcessor` que lê o `X-Correlation-Id` do MDC
  e coloca esse valor nos headers AMQP da mensagem.

- Os event handlers (`PhysicianEventHandler`, `AppointmentEventHandler`) lêem o header `X-Correlation-Id` da mensagem RabbitMQ, voltam a colocá-lo no MDC e incluem o valor nos logs.
- O `EventStoreService` também persiste o correlation ID no event store, permitindo rastrear eventos de auditoria com o mesmo ID de correlação.

Desta forma, é possível seguir nos logs o mesmo `X-Correlation-Id` desde o pedido HTTP de registo de médico/criação de consulta,
passando pela publicação do evento até ao processamento no lado de leitura (MongoDB) e em quaisquer consumidores adicionais
que usem o mesmo header.

### Event Sourcing (Audit Trail)

Para consultas (appointments), o módulo implementa Event Sourcing light:

- Cada operação sobre uma consulta gera um evento no `EventStore`:
  - `CONSULTATION_SCHEDULED` - quando uma consulta é criada
  - `CONSULTATION_UPDATED` - quando uma consulta é atualizada
  - `CONSULTATION_CANCELED` - quando uma consulta é cancelada
  - `NOTE_ADDED` - quando uma nota é adicionada a uma consulta
  - `CONSULTATION_COMPLETED` - quando uma consulta é marcada como concluída
- O endpoint `GET /appointments/{id}/audit-trail` retorna todos os eventos relacionados com uma consulta, permitindo reconstruir o histórico completo.
- O event store persiste metadados como correlation ID, user ID, timestamp e versão do agregado, permitindo auditoria completa.

### Perguntas frequentes (Q&A)

**Q1: Onde é que se vê CQRS no módulo hap-physicians?**
- Comando (escrita): `POST /physicians/register`, `POST /appointments` usam `*CommandService`, que validam regras de negócio, escrevem na base de dados e publicam eventos.
- Query (leitura): `GET /physicians/{id}`, `GET /appointments/{id}` usam `*QueryService`, que lêem de `*QueryRepository` (MongoDB read model) e devolvem DTOs de leitura sem efeitos de escrita.

**Q2: Onde é que se vê AMQP / message broker?**
- Nos command services, após operações bem sucedidas, são publicados eventos via `RabbitTemplate` para um exchange configurado (`hap.rabbitmq.exchange`).
- `PhysicianEventHandler` e `AppointmentEventHandler` ouvem esses eventos e atualizam o read model (MongoDB), garantindo que as queries são rápidas e desacopladas do modelo de escrita.
- Outros serviços podem consumir estes eventos para manter os seus próprios modelos de leitura sincronizados.

**Q3: O CQRS e o AMQP substituem o peer-forwarding?**
- Não. CQRS e AMQP tratam da separação leitura/escrita e da disseminação assíncrona de eventos entre serviços.
- O peer-forwarding continua a ser usado entre instâncias do mesmo serviço (por exemplo em `GET /physicians/{id}` ou `GET /appointments/{id}`) para encontrar dados que ainda não foram replicados localmente.
- Assim, temos dois mecanismos complementares:
  - Eventos AMQP para sincronização entre componentes diferentes (physicians, patients, appointments, auth).
  - Peer-forwarding HTTP para leitura entre instâncias do mesmo componente quando o dado não existe localmente.

**Q4: Estão a usar event sourcing completo ou Sagas no hap-physicians?**
- Para consultas (appointments), sim: usamos um event store (`EventStore`) para auditoria e reconstrução do histórico via `GET /appointments/{id}/audit-trail`. O estado oficial da consulta continua na base de dados relacional, mas o event store permite rastrear todas as mudanças.
- Para médicos (physicians), não: não há event store; apenas eventos AMQP para sincronização do read model.
- Não há uma Saga distribuída formal para o registo de médicos ou criação de consultas; em vez disso, optou-se por integrações síncronas simples com outros serviços e emissão de eventos para integração assíncrona.

**Q5: Por que é que os read models MongoDB são separados por instância?**
- Cada instância tem o seu próprio database MongoDB (`happhysicians_db_1` e `happhysicians_db_2`) para isolamento de dados de leitura.
- Isto permite que cada instância mantenha o seu próprio read model atualizado via eventos, mas pode levar a inconsistências temporárias entre instâncias até que os eventos sejam processados.
- O peer-forwarding ajuda a mitigar este problema ao permitir que uma instância consulte outra se não encontrar dados localmente.

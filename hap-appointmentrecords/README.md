# hap-appointmentrecords — Gestão de Registos de Consultas

Este serviço gere o registo e a consulta de registos clínicos pós-consulta e suporta peer-forwarding entre instâncias para leitura distribuída.

## Perfis e Portas
- instance1 → 8083
- instance2 → 8090

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
- POST /api/appointment-records/{appointmentId}/record
- GET  /api/appointment-records/{recordId}
- GET  /api/appointment-records/patient/mine
- GET  /api/appointment-records/patient/{patientId}

## Colaboração entre serviços (HTTP/REST)
- Physicians: GET http://localhost:{8081}/api/appointments/{id}
- Patients: GET http://localhost:{8082}/patients/{id}
- Auth: POST http://localhost:{8084|8089}/api/public/register
- Propagação de headers (quando aplicável): Authorization, X-User-Id, X-User-Role

## Peer-forwarding
- Se um registo não existir localmente, consulta peers pelas mesmas rotas públicas (evita endpoints internos para o cliente externo; os internos são usados apenas entre instâncias).
- A lista de peers é estática por perfil/instância (ex.: 8083 conhece 8090 e vice-versa) configurada via `hap.appointmentrecords.peers`.
- Se nenhuma instância tiver o recurso, o pedido devolve 404 (não há fallback "mágico").

## Configuração (exemplo)
- Bases URL remotas via application.properties (por profile):
  - hap.physicians.base-url
  - hap.patients.base-url
  - hap.auth.base-url

## Swagger
- http://localhost:8083/swagger-ui.html (instância 1)
- http://localhost:8090/swagger-ui.html (instância 2)

## Decisões e Notas
- **Separação leitura/escrita (CQRS):**
  - Lado comando (escrita) usa JPA + H2 via `AppointmentRecordService` e `AppointmentRecordRepository`.
  - Lado query (leitura) usa MongoDB via `AppointmentRecordProjectionRepository` para respostas otimizadas.
- **Comunicação síncrona (HTTP/REST):**
  - Usada para interagir com o `hap-physicians` (obter detalhes de consultas) e `hap-patients` (obter detalhes de pacientes).
  - Usada também entre containers quando é necessária consistência imediata (ex.: validação de consulta antes de criar registo).
  - Implementado com Circuit Breaker (Resilience4j) para tolerância a falhas de serviços externos.
- **Comunicação assíncrona (AMQP/RabbitMQ):**
  - `AppointmentEventsListener` consome eventos de consultas (`AppointmentCreatedEvent`, `AppointmentCompletedEvent`, etc.) do exchange `hap-exchange`.
  - Estes eventos são usados principalmente para logging e tracing; o serviço funciona como participante passivo na coreografia.
  - Opcionalmente, após criar um registo, pode publicar `AppointmentRecordCreatedEvent` (configurável via `hap.events.records.enabled`).
- **Single Source of Truth:**
  - O serviço não mantém uma cópia local da entidade `Appointment`; apenas armazena o `appointmentId` como referência.
  - Detalhes de consultas são obtidos em tempo real via HTTP do serviço `hap-physicians` quando necessário.
  - Esta abordagem evita problemas de sincronização e mantém a consistência dos dados.
- **Peer-forwarding HTTP entre instâncias:**
  - Continua ativo mesmo com CQRS/AMQP: se a instância local ainda não conhece o registo, tenta sequencialmente os peers configurados.
  - Implementado no `AppointmentRecordController` utilizando `RestTemplate` para lidar com falhas temporárias de peers.
  - Previne loops infinitos através do header `X-Peer-Request`.
- **Isolamento entre serviços:**
  - Não há imports diretos de classes de outros módulos; a integração é sempre via HTTP/REST ou eventos AMQP.
- **Resiliência:**
  - Circuit Breaker (Resilience4j) aplicado nas chamadas HTTP ao serviço `hap-physicians` via `ExternalServiceClient`.
  - Permite que o serviço continue a funcionar mesmo quando serviços externos estão temporariamente indisponíveis.

## Limitações conhecidas
- Service Discovery estático (via lista de peers no application.properties). Implementação de resiliência customizada para tolerância a falhas de rede entre instâncias.
- Sem cache distribuída; consistência eventual entre instâncias.
- Eventos focados nos cenários principais (por exemplo, `AppointmentRecordCreatedEvent`); extensões para outros eventos são possíveis mas não totalmente exploradas aqui.
- O módulo não aplica event sourcing completo: o estado oficial do registo está numa base relacional e as projeções em MongoDB são atualizadas de forma síncrona após escrita.
- Dependência de serviços externos (hap-physicians, hap-patients) para obter detalhes completos; se estes serviços estiverem indisponíveis, algumas funcionalidades podem ser limitadas (mitigado pelo Circuit Breaker).

## Testes e build
```cmd
mvnw.cmd -q test
mvnw.cmd -q -DskipTests package
```

## CQRS

- Na nossa implementação Java com Spring Boot, os conceitos de CQRS foram mapeados da seguinte forma:
- Os Commands (ex: CreateAppointmentRecord) são representados pelos métodos transacionais no `AppointmentRecordService`, que atuam sobre o modelo de escrita (JPA / base de dados relacional do serviço de registos).
- As Queries (ex: GetAppointmentRecordById) são representadas pelos métodos de leitura que consultam o modelo de leitura (`AppointmentRecordProjectionRepository` / projeções de leitura em MongoDB).
- Os DTOs de entrada (por exemplo `AppointmentRecordRequest`) funcionam como objetos de comando.

## Messaging e Tracing no hap-appointmentrecords

Este módulo usa RabbitMQ para consumir eventos de consultas (`AppointmentCreatedEvent`, `AppointmentCompletedEvent`, etc.) e opcionalmente publicar eventos de registos criados (`AppointmentRecordCreatedEvent`).
Os eventos são consumidos por `AppointmentEventsListener`, que os utiliza principalmente para logging e tracing.
Além dos logs, o sistema integra com o Zipkin (via Micrometer Tracing) para visualização gráfica das spans e latências. O X-Correlation-Id serve como TraceId, permitindo depurar o fluxo completo: REST Request -> RabbitMQ Consume -> HTTP External Service Call -> MongoDB Write.

### Correlation IDs (Tracing de ponta a ponta)

Para permitir rastreio de um pedido entre serviços:

- O controlador `AppointmentRecordController` aceita opcionalmente o header HTTP `X-Correlation-Id`.
    - Se não existir, gera um UUID e coloca-o no MDC (contexto de logging) sob a mesma chave.
- O `RabbitTemplate` é configurado em `RabbitMQConfig` com um `beforePublishPostProcessor` que lê o `X-Correlation-Id` do MDC
  e coloca esse valor nos headers AMQP da mensagem.
- O `AppointmentEventsListener` lê o header `X-Correlation-Id` da mensagem RabbitMQ, volta a colocá-lo no MDC e inclui o valor nos logs.

Desta forma, é possível seguir nos logs o mesmo `X-Correlation-Id` desde o pedido HTTP de criação de registo,
passando pela publicação do evento (se aplicável) até ao processamento no lado de leitura (MongoDB) e em quaisquer consumidores adicionais
que usem o mesmo header.

### Perguntas frequentes (Q&A)

**Q1: Onde é que se vê CQRS no módulo hap-appointmentrecords?**
- Comando (escrita): `POST /api/appointment-records/{appointmentId}/record` usa `AppointmentRecordController` + `AppointmentRecordService`, que valida regras de negócio, escreve na base de dados de registos (H2) e atualiza projeções em MongoDB.
- Query (leitura): `GET /api/appointment-records/{recordId}` usa `AppointmentRecordController` + `AppointmentRecordService`, que lê de `AppointmentRecordProjectionRepository` (MongoDB read model) e devolve um DTO de leitura sem efeitos de escrita.

**Q2: Onde é que se vê AMQP / message broker?**
- O `AppointmentEventsListener` consome eventos de consultas do exchange `hap-exchange` via RabbitMQ.
- Estes eventos são usados principalmente para logging e tracing; o serviço funciona como participante passivo.
- Opcionalmente, `AppointmentEventsPublisher` pode publicar `AppointmentRecordCreatedEvent` quando um registo é criado (configurável via `hap.events.records.enabled`).

**Q3: O CQRS e o AMQP substituem o peer-forwarding?**
- Não. CQRS e AMQP tratam da separação leitura/escrita e da disseminação assíncrona de eventos entre serviços.
- O peer-forwarding continua a ser usado entre instâncias do mesmo serviço (por exemplo em `GET /api/appointment-records/{recordId}`) para encontrar dados que ainda não foram replicados localmente.
- Assim, temos dois mecanismos complementares:
  - Eventos AMQP para sincronização entre componentes diferentes (appointments, records, patients, physicians).
  - Peer-forwarding HTTP para leitura entre instâncias do mesmo componente quando o dado não existe localmente.

**Q4: Por que é que o serviço não mantém uma cópia local da entidade Appointment?**
- Para respeitar o **Single Source of Truth**. Manter cópias de dados de outros serviços cria problemas complexos de sincronização (consistência eventual).
- Referenciar por ID + consulta em tempo real via HTTP é mais robusto para este caso de uso e evita duplicação de dados.

**Q5: O que acontece se o serviço hap-physicians estiver indisponível?**
- O Circuit Breaker (Resilience4j) aplicado nas chamadas HTTP via `ExternalServiceClient` previne que o serviço fique bloqueado.
- Quando o Circuit Breaker está aberto, as chamadas falham rapidamente sem esperar timeouts, permitindo que o serviço continue a funcionar para outras operações.
- Algumas funcionalidades que dependem de dados do hap-physicians podem estar limitadas, mas o serviço não fica completamente indisponível.

## Segurança, mTLS e Autorização
- OAuth2/JWT como resource-server com regras por role (DOCTOR/ADMIN para notas, PATIENT/DOCTOR/ADMIN para endpoints de paciente; restante autenticado).
- mTLS ativável: client auth `need` nos perfis instance1/instance2, truststore/keystore PKCS12 em `src/main/resources/certs` (senha `secretpassword`).
- Compose monta `/certs` para auth/physicians/appointmentrecords blue/green; cert SANs devem cobrir hostnames dos containers.
- Para chamadas internas, URLs usam `https://`; clientes devem apresentar certificado.

## Blue-Green e Canary (AppointmentRecords)
- Serviços: `hap-appointmentrecords-blue` (8083), `hap-appointmentrecords-green` (8090), proxy nginx em 8085.
- Troca de tráfego via script: `pwsh ./scripts/switch-appointmentrecords-traffic.ps1 -BlueWeight 0 -GreenWeight 100` (ou `-Canary -Step 10 -GreenWeight 30` para rampa). Script edita `hap-appointmentrecords/nginx.conf` e faz reload do proxy.

## Event Sourcing e Saga
- Eventos consumidos (`appointment.created/updated/canceled`) são guardados em Mongo (`appointment_event_store`) com payload JSON e correlationId.
- Saga log em `saga_events` com status IN_PROGRESS/COMPLETED/COMPENSATED; cancelamentos disparam `COMPENSATION_TRIGGERED` e marcam estado compensado.
- Replay: `EventReplayer.rebuildProjections(appointmentId)` reconstrói a projeção em Mongo a partir do stream.

## Considerações de Privacidade (GDPR)
- Evitar logging de PII (diagnóstico/notas) fora de DEBUG; habilitar mTLS para proteger dados em trânsito.
- Tracing/CorrelationId ativo; revisar retenção de logs/metrics conforme política da organização.

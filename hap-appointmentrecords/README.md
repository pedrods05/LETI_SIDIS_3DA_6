# HAP-APPOINTMENTRECORDS

## Overview

O módulo HAP-AppointmentRecords é responsável pela gestão de registos de consultas no sistema hospitalar. Este módulo funciona como um microserviço independente que permite a criação e visualização de registos de consultas, comunicando com outros módulos via HTTP/REST e (a partir desta iteração) via eventos AMQP (RabbitMQ) para sincronização de projeções (CQRS).

## Tecnologias Utilizadas

- Spring Boot 3.5.6
- Spring Data JPA
- Spring Security
- Spring Web (RestTemplate)
- Spring AMQP (RabbitMQ)
- H2 Database (in-memory)
- Lombok
- Maven

---

## O que foi implementado nesta iteração

Resumo das principais alterações funcionais e arquiteturais aplicadas a este módulo:

- Implementação inicial de CQRS (Command / Query Responsibility Segregation):
  - Write side: comandos e handlers (ex.: `CreateAppointmentCommand` + `CreateAppointmentCommandHandler`).
  - Read side: projeção/desnormalized view (`AppointmentProjection`) e repositório (`AppointmentProjectionRepository`).
- Integração AMQP (RabbitMQ):
  - Configuração AMQP (`config/RabbitConfig`, `config/AmqpProperties`).
  - Publicação de eventos pelo handler de comando (`RabbitTemplate` com `Jackson2JsonMessageConverter`).
  - Listener/consumer que processa eventos e atualiza a projeção (`AppointmentEventsListener`).
- Testes unitários adicionados para as partes críticas:
  - `CreateAppointmentCommandHandlerTest` (verifica persistência e publicação do evento).
  - `AppointmentEventsListenerTest` (verifica que a projeção e o write-model são gravados).

Arquivos/Classes importantes adicionados/alterados (caminhos relativos a `src/main/java`):

- `leti_sisdis_6.hapappointmentrecords.config.AmqpProperties`
- `leti_sisdis_6.hapappointmentrecords.config.RabbitConfig`
- `leti_sisdis_6.hapappointmentrecords.service.command.CreateAppointmentCommand`
- `leti_sisdis_6.hapappointmentrecords.service.command.CreateAppointmentCommandHandler`
- `leti_sisdis_6.hapappointmentrecords.service.event.AppointmentCreatedEvent`
- `leti_sisdis_6.hapappointmentrecords.service.event.AppointmentEventsListener`
- `leti_sisdis_6.hapappointmentrecords.model.AppointmentProjection`
- `leti_sisdis_6.hapappointmentrecords.repository.AppointmentProjectionRepository`

---

## Como funciona (visão rápida)

1. O cliente chama o endpoint que dispara um comando (Write side).
2. O `CreateAppointmentCommandHandler` valida/ persiste no banco transacional (write-model) e publica um `AppointmentCreatedEvent` no exchange configurado.
3. O `AppointmentEventsListener` (ou qualquer outro consumidor interessado) consome o evento e constrói/atualiza uma projeção (`AppointmentProjection`) usada para consultas rápidas (read-model).

Isto garante desacoplamento entre produtores e consumidores e permite otimizar modelos de leitura para as APIs Experience sem penalizar as operações transacionais.

---

## Configuração AMQP (Resumo)

- Propriedades (em `src/main/resources/application.properties`):
  - `spring.rabbitmq.host`, `spring.rabbitmq.port`, `spring.rabbitmq.username`, `spring.rabbitmq.password`
  - `app.amqp.exchange` (ex.: `hap.appointments.exchange`)
  - `app.amqp.queue.appointment-events` (ex.: `hap.appointments.queue`)
  - `app.amqp.routing-key.appointment.created` (ex.: `appointment.created`)

- Beans principais:
  - `TopicExchange` (exchange configurada via `AmqpProperties`)
  - `Queue` e `Binding` (fila ligada à routing key)
  - `Jackson2JsonMessageConverter` + `RabbitTemplate` configurados para serializar eventos em JSON

Observação: o método consumidor `onAppointmentCreated(AppointmentCreatedEvent)` existe e é público; para ativar o consumo automático via Spring AMQP basta anotar o método/componente com `@RabbitListener(queues = "${app.amqp.queue.appointment-events}")` (ou manter listeners separados). A configuração AMQP já foi adicionada no `pom.xml` (dependência `spring-boot-starter-amqp`).

---

## Como testar localmente

A seguir há instruções passo-a-passo para verificar o funcionamento (unit tests e smoke test end-to-end com RabbitMQ). Os comandos são para PowerShell no Windows.

1) Executar unit tests (rápido)

Abra PowerShell na raiz do repositório (`C:\IdeaProjects\LETI_SIDIS_3DA_6`) e execute:

```powershell
& ".\hap-appointmentrecords\mvnw.cmd" -f ".\hap-appointmentrecords" clean test
```

Se preferires usar o Maven instalado globalmente:

```powershell
mvn -f hap-appointmentrecords clean test
```

O que verificar: os testes `CreateAppointmentCommandHandlerTest` e `AppointmentEventsListenerTest` devem passar.

2) Smoke test end-to-end com RabbitMQ (Docker)

a) Subir RabbitMQ (imagem com management UI):

```powershell
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

b) Arrancar a aplicação `hap-appointmentrecords` (usando mvnw):

```powershell
& ".\hap-appointmentrecords\mvnw.cmd" -f ".\hap-appointmentrecords" spring-boot:run
```

A app inicia na porta `8083` por defeito (ver `application.properties`). Verifica os logs para confirmar que a configuração AMQP foi carregada (exchange/queue/binding).

c) Publicar uma mensagem de teste usando a API HTTP do RabbitMQ (management)

Exemplo de payload JSON para o evento `AppointmentCreatedEvent`:

```json
{"appointmentId":"a1","patientId":"p1","physicianId":"d1","dateTime":"2025-12-10T09:00:00","consultationType":"FIRST_TIME","status":"SCHEDULED","occurredAt":"2025-12-04T12:34:56"}
```

Comandos PowerShell para publicar via HTTP API do RabbitMQ (management):

```powershell
$inner = '{"appointmentId":"a1","patientId":"p1","physicianId":"d1","dateTime":"2025-12-10T09:00:00","consultationType":"FIRST_TIME","status":"SCHEDULED","occurredAt":"2025-12-04T12:34:56"}'
$body = @{ properties = @{}; routing_key = 'appointment.created'; payload = $inner; payload_encoding = 'string' } | ConvertTo-Json -Depth 5
$uri = 'http://localhost:15672/api/exchanges/%2F/hap.appointments.exchange/publish'
$cred = New-Object System.Management.Automation.PSCredential('guest',(ConvertTo-SecureString 'guest' -AsPlainText -Force))
Invoke-RestMethod -Uri $uri -Method Post -Credential $cred -Body $body -ContentType 'application/json'
```

d) Verificar a projeção (H2 Console)

- Aceder a: http://localhost:8083/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- User: `sa` / password: (vazio)
- Executar: `SELECT * FROM appointments_projection;`

Se tudo correu bem, deverás ver uma linha com `appointment_id = 'a1'`.

---

## Troubleshooting

- Problema comum no Windows ao executar o `mvnw.cmd`: se o caminho do Java/usuário contiver espaços, o wrapper pode falhar com mensagens tipo `"C:\Users\Jos' is not recognized as an internal or external command"`.
  - Solução: invoca o wrapper com o operador de call `&` e paths entre aspas (ex.: `& ".\hap-appointmentrecords\mvnw.cmd" -f ".\hap-appointmentrecords" clean test`).
  - Alternativa: instalar Maven globalmente e executar `mvn -f hap-appointmentrecords clean test`.

- Se o listener não processa mensagens:
  - Confirma que o bean `RabbitConfig` foi carregado (procura nos logs exchange/queue/binding).
  - Se preferires auto-binding, anota o método de listener com `@RabbitListener` e garante `@EnableRabbit` numa configuração (por exemplo `RabbitConfig` ou a classe principal).
  - Verifica que o `payload` enviado está em `payload_encoding=string` ou ajusta para `payload_encoding=base64` conforme necessário.

---

## Notas operacionais e próximos passos

- Produção:
  - Usa credenciais seguras e TLS para RabbitMQ.
  - Configura DLQ (Dead Letter Queues) e retry policies para mensagens que falham repetidamente.
  - Considera usar Testcontainers em CI para validar integração com RabbitMQ.

- Evolução arquitetural:
  - Implementar idempotência no handler (ex.: stored event ids ou upsert com timestamp).
  - Adicionar eventos adicionais: `AppointmentUpdatedEvent`, `AppointmentCanceledEvent`.
  - Considerar Event Store / Event Sourcing se precisarmos de replays e auditoria completa.

---

## Referências

- Arquivo de decisão arquitetura: `../2ITERAÇÃO.md`
- Código fonte: `src/main/java/leti_sisdis_6/hapappointmentrecords/`

---

Obrigado — se quiseres, posso:

- (A) re-ativar `@RabbitListener` no `AppointmentEventsListener` e criar um teste de integração com Testcontainers RabbitMQ; ou
- (B) gerar um ficheiro PowerShell (`smoke-test-rabbit.ps1`) que automatiza o deploy do RabbitMQ via Docker, arranca a app e publica o evento de exemplo; ou
- (C) adicionar logs informativos no `onAppointmentCreated` para facilitar debugging durante o smoke test.

Diz qual opção preferes e eu faço a alteração seguinte.

# HAP-APPOINTMENTRECORDS

## Eventos (AMQP) e CQRS — Atualizado

Esta secção documenta, de forma explícita, os eventos AMQP que este módulo produz e consome, o que acontece em cada caso, e as limitações atuais de tratamento de erros. Também esclarece como os IDs de correlação (correlationId) são propagados para permitir tracing end‑to‑end.

### Exchange e Convenções
- Exchange: `hap-appointmentrecords-exchange` (configurável via `hap.rabbitmq.exchange` em `application.properties`).
- Tipo: `topic`.
- Header de correlação: `X-Correlation-Id` (injetado automaticamente a partir do MDC quando existe, e gerado se faltar).

### Eventos Produzidos
- Routing key: `appointment.created`
  - Onde é publicado: `AppointmentEventsPublisher` (chamado a partir de `AppointmentRecordService#createRecord`).
  - Quando: após criar um registo de consulta (write-model JPA), publica um evento de criação de consulta para atualizar projeções/read-model noutros serviços.
  - Payload (JSON):
    - `appointmentId` (string)
    - `patientId` (string)
    - `physicianId` (string)
    - `dateTime` (ISO-8601)
    - `consultationType` (enum)
    - `status` (enum)
    - `occurredAt` (ISO-8601)
  - Headers:
    - `X-Correlation-Id`: propagado do pedido HTTP ou gerado no publisher.
  - Logging no envio (exemplo):
    - `⚡ Evento AppointmentCreatedEvent enviado | correlationId=<uuid> | appointmentId=<id>`

#### Eventos emitidos por este módulo (Registos clínicos) — possível evolução
- Estado atual: este módulo NÃO emite eventos próprios de registos clínicos por omissão, pois não há dependentes diretos conhecidos desses dados.
- Evolução proposta (se surgir consumidor):
  - Nome/Routing key sugerida: `appointmentrecord.created` (ou `appointment.record.created` seguindo o padrão da equipa).
  - Publisher: serviço de registos após criar o write‑model e a projeção.
  - Payload (privacidade‑consciente; enviar apenas o necessário):
    - `recordId` (string)
    - `appointmentId` (string)
    - `patientId` (string) — opcional; avaliar necessidade/mascarar
    - `physicianId` (string) — opcional
    - `occurredAt` (ISO‑8601)
  - Headers: inclui `X-Correlation-Id` tal como nos restantes eventos.
  - Potenciais consumidores: analytics, auditoria, notificações.
  - Notas:
    - Evitar dados clínicos sensíveis no payload (diagnóstico, prescrições). Se inevitável, considerar encriptação ou redigir apenas metadados.
    - Tornar o consumidor/idempotência explícitos (ex.: chave por `recordId`).

### Eventos Consumidos
- Routing key: `appointment.created`
  - Consumer: `AppointmentEventsListener#onAppointmentCreated`
  - Efeito:
    - Atualiza/insere a projeção de leitura `AppointmentProjection` (Mongo) com os dados do evento.
    - Mantém uma cópia local do write-model `Appointment` (JPA) para coerência local.
  - Correlation/tracing:
    - O listener extrai `X-Correlation-Id` dos headers e coloca-o no MDC.
  - Logging na receção (exemplo):
    - `📥 Evento AppointmentCreatedEvent recebido | correlationId=<uuid> | appointmentId=<id>`

- Routing key: `appointment.canceled` (planeado)
  - Estado: não implementado neste módulo nesta iteração.
  - Intenção futura:
    - Marcar `status=CANCELLED` na `AppointmentProjection` (Mongo) e refletir no write-model local se necessário.

### Tratamento de Erros no Listener
- Comportamento atual:
  - O listener regista logs informativos e processa a mensagem. Em caso de exceção não tratada, a exceção propaga-se ao container AMQP. O comportamento por omissão do Spring AMQP pode reencaminhar a mensagem para retry/requeue, o que pode causar reprocessamentos repetidos (poison message) sem DLQ.
- Limitações conhecidas (aceites nesta fase):
  - Não há DLQ (Dead Letter Queue) configurada.
  - Não há política de retry/backoff personalizada.
  - Não há idempotência explícita no consumidor.
- Próximos passos sugeridos:
  - Configurar DLQ/bindings para `appointment.*` e políticas de retry.
  - Tornar operações idempotentes (e.g., upsert com versionamento/`occurredAt`).
  - Adicionar métricas/alertas para falhas no consumo.

### CQRS: Read vs Write
- Write-model (JPA/H2):
  - `Appointment`, `AppointmentRecord` persistidos via repositórios JPA.
  - Endpoints de escrita continuam a usar JPA para a fonte de verdade.
- Read-model (Mongo):
  - `AppointmentProjection` e `AppointmentRecordProjection` lidas via repositórios Mongo.
  - Endpoints de leitura usam exclusivamente as projeções Mongo para respostas rápidas e estáveis.

### Onde encontrar no código
- Correlation IDs:
  - HTTP → MDC: `config/CorrelationIdFilter.java`.
  - Publish AMQP → header: `config/RabbitMQConfig.java` (post-processor) e `service/event/AppointmentEventsPublisher.java`.
  - Receive AMQP → MDC/logs: `service/event/AppointmentEventsListener.java`.
- Projeções/read-model:
  - `model/AppointmentProjection.java`, `repository/AppointmentProjectionRepository.java`.
  - `model/AppointmentRecordProjection.java`, `repository/AppointmentRecordProjectionRepository.java`.
- Endpoints:
  - Appointments (leitura via projeções): `api/AppointmentQueryController.java`.
  - Appointment records (leitura via projeções): `service/AppointmentRecordService.java` (métodos read-only chamados por `api/AppointmentRecordController.java`).

---

Nota: Secções mais antigas deste README que referem handlers/commands com nomes diferentes podem estar desatualizadas; a lista acima reflete o estado real do código nesta iteração.

## Padrão Saga (Coreografia vs. Orquestração)

Neste módulo, não faz sentido implementar uma Saga complexa por orquestração. O ciclo de marcação/alteração/cancelamento de consultas é liderado por serviços que detêm esse domínio (ex.: physicians) e interagem com patients. O `hap-appointmentrecords` atua como participante passivo numa saga coreografada.

- Papel neste serviço (participante passivo):
  - Consome eventos de negócio (ex.: `appointment.created`; futuramente `appointment.updated`, `appointment.canceled`).
  - Atualiza o seu próprio estado/read‑model (projeções Mongo) e mantém coerência local com o write‑model quando aplicável.
  - Não inicia nem coordena transações distribuídas; não chama compensações noutros serviços.

- Por que não orquestrar aqui:
  - Ownership: marcação e ciclo de vida da consulta pertencem ao bounded context de scheduling/physicians; este módulo não deve decidir fluxos globais.
  - Acoplamento: um orquestrador aqui criaria dependências cruzadas desnecessárias e reduziria a autonomia dos outros serviços.
  - Requisitos: leitura de registos clínicos tolera consistência eventual; a experiência não exige coordenação síncrona multi-serviço.

- Erros e consistência (estado atual):
  - Consumo AMQP com pelo menos‑uma‑vez; recomenda‑se upsert idempotente nas projeções (já suportado pelo design atual) e planeamento de DLQ/retry para produção.
  - Se o listener falhar, o comportamento por omissão pode reentregar; sem DLQ configurada, isto é uma limitação conhecida documentada.

- Como explicar na defesa (script breve):
  - "O hap-appointmentrecords funciona como um participante passivo numa Saga coreografada. Ele reage aos eventos publicados pelos serviços que detêm o ciclo de vida da consulta (como o physicians) e atualiza o seu read model (Mongo) para servir consultas rápidas. Não coordena o fluxo global nem executa compensações noutros serviços; isso mantém baixo acoplamento e respeita os bounded contexts. Como as leituras toleram consistência eventual, a coreografia é suficiente e mais simples para este domínio."

## Fronteiras e Acesso a Dados (Contracto de Integração)

- Não existe acesso direto à base de dados deste módulo por outros serviços. A BD é privada ao bounded context `hap-appointmentrecords` (database-per-service).
- A comunicação inter-serviços é feita exclusivamente por:
  - HTTP/REST (endpoints públicos documentados) e
  - Eventos AMQP (RabbitMQ) publicados/consumidos (ver secção de Eventos).
- Não partilhamos drivers, strings de conexão nem credenciais com outros serviços; não há dependências de repositórios cruzados que apontem para esta BD.
- Operacionalmente:
  - Em desenvolvimento, as BDs locais estão ligadas a `localhost` e só a própria app tem as credenciais.
  - Em Docker/Compose, a BD deste serviço não deve ser publicada como porta externa; usar rede interna do compose e credenciais dedicadas.

## Database per instance / per service (pragmático em desenvolvimento)

- Write‑model (JPA/H2):
  - Configuração atual usa H2 in‑memory (`jdbc:h2:mem:testdb`), o que já fornece uma base “por instância de JVM” em desenvolvimento.
  - Isto cumpre razoavelmente o espírito de “database per instance” para a cadeira.
  - Opcional (mais purista): usar H2 file‑based com ficheiro único por instância, por exemplo usando a porta como sufixo do path.
    - Exemplo (comentado):
      - `# spring.datasource.url=jdbc:h2:file:./data/records-${server.port};DB_CLOSE_DELAY=-1;MODE=PostgreSQL`
- Read‑model (Mongo):
  - A BD usada é `hapappointmentrecords_db` (nome dedicado por serviço), suficiente para "database per service".
  - Múltiplas instâncias da app podem partilhar este read‑model, já que é uma projeção. Se preferires isolar por instância em dev, poderias sufocar com o perfil/porta.
    - Exemplo (comentado):
      - `# spring.data.mongodb.uri=mongodb://user:pass@localhost:27017/hapappointmentrecords_db_${server.port}?authSource=admin`

Nota: Não é obrigatório para esta cadeira usar ficheiros H2 por instância ou BDs Mongo distintas por instância; as sugestões acima são apenas para quem quer ser mais purista em isolamento durante o desenvolvimento.

## Documentação específica deste módulo

### Peer forwarding & multi-instância
- Este módulo suporta execução em multi-instância e encaminhamento peer‑to‑peer.
- Quando um registo clínico não é encontrado localmente em `GET /api/appointment-records/{id}`:
  - Consulta a lista de peers (`ExternalServiceClient#getPeerUrls`).
  - Encaminha para o endpoint interno dos peers: `GET {peer}/internal/appointment-records/{id}`.
  - Usa o cabeçalho `X-Peer-Request: true` para evitar loops e reencaminha `Authorization` se existir.
  - Se algum peer devolver 2xx com corpo, responde ao cliente com esse resultado; caso contrário devolve 404.
- Logs emitidos durante o fallback:
  - `Appointment record not found locally, querying peers: [http://inst2,...]`
  - `Appointment record found on peer: http://inst2`
  - `Appointment record not found in any peer`.

### Papel na coreografia (Saga coreografada)
- Este serviço é um participante passivo na coreografia de eventos do domínio:
  - Consome eventos `appointment.created` publicados por serviços que gerem a marcação (ex.: physicians).
  - Atualiza/insere a projeção de leitura (`AppointmentProjection`) em Mongo e mantém coerência local no write‑model JPA.
- Foco funcional: registos clínicos (detalhes de consulta), não a marcação de consultas.
- Possível evolução de emissão de eventos próprios:
  - `AppointmentRecordCreatedEvent` (desativado por omissão; pode ser ativado via propriedade) com payload mínimo e cabeçalho `X-Correlation-Id`.

### Limitações e Evolução futura
- Event Sourcing completo: não implementado. Há padrões de eventos + projeção (CQRS) suficientes para esta cadeira.
- Saga formal (orquestrador): não existe aqui; opta‑se por coreografia simples com baixo acoplamento.
- Correlation IDs em AMQP:
  - Já se propaga `X-Correlation-Id` (HTTP→MDC→AMQP headers) e há logs de envio/receção.
  - Pode ser aprofundado com métricas/traços adicionais (Zipkin/OTel), DLQ e políticas de retry em produção.
- Resiliência:
  - Chamadas HTTP a Auth/Physicians/Patients usam `@Retryable` + `@CircuitBreaker` (Resilience4j) e logs detalhados de falha.
- Observabilidade:
  - Actuator (health, info, metrics) e traçado via Zipkin configurável (`management.zipkin.tracing.endpoint`).


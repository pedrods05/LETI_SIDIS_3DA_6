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

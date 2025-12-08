# hap-patients — Gestão de Pacientes

Este serviço gere o registo e a consulta de pacientes e suporta peer-forwarding entre instâncias para leitura distribuída.

## Perfis e Portas
- instance1 → 8082
- instance2 → 8088

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
- GET  /patients/{id}
- POST /api/v2/patients/register

## Testes

### Executar todos os testes
```bash
mvn test
```

### Executar testes específicos
```bash
mvn test -Dtest=PatientServiceTest
mvn test -Dtest=PatientEventHandlerTest
```

### Cobertura de Testes
O módulo contém **15 classes de teste** cobrindo:
- ✅ Controllers (REST endpoints + peer forwarding)
- ✅ Services (business logic + CQRS commands)
- ✅ Repositories (H2 + MongoDB)
- ✅ Event Handlers (RabbitMQ consumers)
- ✅ Configuration (RabbitMQ + HTTP Client)
- ✅ Models & DTOs
- ✅ Exception Handling

📄 **Documentação completa dos testes:** [TEST_DOCUMENTATION.md](./TEST_DOCUMENTATION.md)

## Colaboração entre serviços (HTTP/REST)
- Auth: POST http://localhost:{8084|8089}/api/public/register
- Propagação de headers (quando aplicável): Authorization, X-User-Id, X-User-Role

## Peer-forwarding
- Se um paciente não existir localmente, consulta peers pelas mesmas rotas públicas (evita endpoints internos para o cliente externo; os internos são usados apenas entre instâncias).
- A lista de peers é estática por perfil/instância (ex.: 8082 conhece 8088 e vice-versa) configurada via `hap.patients.peers`.
- Se nenhuma instância tiver o recurso, o pedido devolve 404 (não há fallback "mágico").

## Configuração (exemplo)
- Bases URL remotas via application.properties (por profile):
  - hap.auth.base-url

## Swagger
- http://localhost:8082/swagger-ui.html (instância 1)
- http://localhost:8088/swagger-ui.html (instância 2)

## Decisões e Notas
- **Separação leitura/escrita (CQRS):**
  - Lado comando (escrita) usa JPA + H2  via `PatientRegistrationService` e `PatientRepository`.
  - Lado query (leitura) usa MongoDB via `PatientQueryService` e `PatientQueryRepository` para respostas otimizadas.
- **Comunicação síncrona (HTTP/REST):**
  - Usada para interagir com o `hap-auth` (`/api/public/register`, `/api/public/login`).
  - Usada também entre containers quando é necessária consistência imediata (ex.: physicians/appt-records a pedir detalhes de pacientes).
- **Comunicação assíncrona (AMQP/RabbitMQ):**
  - Após um registo bem-sucedido, `PatientRegistrationService` publica `PatientRegisteredEvent` no exchange `hap-exchange`.
  - `PatientEventHandler` consome esses eventos e atualiza o modelo de leitura em MongoDB.
  - Este padrão permite que outros serviços (physicians, appointment-records) também reajam a eventos sem acoplamento forte.
- **Event log / Event Sourcing light:**
  - Para cada registo de paciente, o serviço persiste um `PatientEvent` na tabela `patient_events` com tipo de evento, `patientId` e metadados de auditoria.
  - O estado oficial do paciente continua na entidade JPA `Patient`; o event log funciona como trilho de auditoria (audit trail) e primeiro passo em direção a event sourcing, mas não é usado para reconstruir o estado.
- **Peer-forwarding HTTP entre instâncias:**
  - Continua ativo mesmo com CQRS/AMQP: se a instância local ainda não conhece o paciente, tenta sequencialmente os peers configurados.
  - Implementado no `PatientController` utilizando `ResilientRestTemplate` para lidar com falhas temporárias de peers.
- **Isolamento entre serviços:**
  - Não há imports diretos de classes de outros módulos; a integração é sempre via HTTP/REST ou eventos AMQP.
- **Sagas envolvendo pacientes:**
  - Não existe uma Saga distribuída formal para o registo de pacientes. A criação de credenciais em `hap-auth` é feita via chamada HTTP síncrona dentro de uma transação local.
  - Os eventos `PatientRegisteredEvent` ficam disponíveis para que outros serviços possam reagir (coreografia leve), mas sem um orquestrador de Saga nem passos de compensação.

## Limitações conhecidas
- Service Discovery estático (via lista de peers no application.properties). Implementação de resiliência customizada (ResilientRestTemplate) para tolerância a falhas de rede entre instâncias, em vez de um Circuit Breaker de biblioteca (Resilience4j) neste módulo específico."
- Sem cache distribuída; consistência eventual entre instâncias.
- Eventos focados nos cenários principais (por exemplo, `PatientRegisteredEvent`); extensões para outros eventos são possíveis mas não totalmente exploradas aqui.
- O módulo não aplica event sourcing completo: o estado oficial do paciente está numa base relacional e o event log (`PatientEvent`) não é usado para reconstruir o estado.
- Não há Saga de registo de paciente com vários passos assíncronos e compensações; optou-se por um fluxo mais simples (transação local + chamada HTTP + eventos de integração).

## Testes e build
```cmd
mvnw.cmd -q test
mvnw.cmd -q -DskipTests package
```

## CQRS

- Na nossa implementação Java com Spring Boot, os conceitos de CQRS foram mapeados da seguinte forma:
- Os Commands (ex: RegisterPatient) são representados pelos métodos transacionais no `PatientRegistrationService`, que atuam sobre o modelo de escrita (JPA / base de dados relacional do serviço de pacientes).
- As Queries (ex: GetPatientById) são representadas pelos métodos de leitura no `PatientQueryService`, que consultam o modelo de leitura (`PatientQueryRepository` / projeções de leitura).
- Os DTOs de entrada (por exemplo `PatientRegistrationDTOV2`) funcionam como objetos de comando.

## Messaging e Tracing no hap-patients

- Este módulo usa RabbitMQ para publicar o evento `PatientRegisteredEvent` sempre que um novo paciente é registado.
- O evento é consumido localmente por `PatientEventHandler`, que atualiza o modelo de leitura em MongoDB (`PatientSummary`).
- Além dos logs, o sistema integra com o Zipkin (via Micrometer Tracing) para visualização gráfica das spans e latências. O X-Correlation-Id serve como TraceId, permitindo depurar o fluxo completo: REST Request -> RabbitMQ Publish -> RabbitMQ Consume -> MongoDB Write.

### Correlation IDs (Tracing de ponta a ponta)

Para permitir rastreio de um pedido entre serviços:

- O controlador `PatientRegistrationController` aceita opcionalmente o header HTTP `X-Correlation-Id`.
    - Se não existir, gera um UUID e coloca-o no MDC (contexto de logging) sob a mesma chave.
- O `RabbitTemplate` é configurado em `RabbitMQConfig` com um `beforePublishPostProcessor` que lê o `X-Correlation-Id` do MDC
  e coloca esse valor nos headers AMQP da mensagem.
- O `PatientEventHandler` lê o header `X-Correlation-Id` da mensagem RabbitMQ, volta a colocá-lo no MDC e inclui o valor nos logs.

Desta forma, é possível seguir nos logs o mesmo `X-Correlation-Id` desde o pedido HTTP de registo de paciente,
passando pela publicação do evento até ao processamento no lado de leitura (MongoDB) e em quaisquer consumidores adicionais
que usem o mesmo header.
### Perguntas frequentes (Q&A)

**Q1: Onde é que se vê CQRS no módulo hap-patients?**
- Comando (escrita): `POST /api/v2/patients/register` usa `PatientRegistrationController` + `PatientRegistrationService`, que valida regras de negócio, escreve na base de dados de pacientes e publica eventos.
- Query (leitura): `GET /patients/{id}` usa `PatientController` + `PatientQueryService`, que lê de `PatientQueryRepository` (MongoDB read model) e devolve um DTO de leitura sem efeitos de escrita.

**Q2: Onde é que se vê AMQP / message broker?**
- No `PatientRegistrationService`, após um registo bem sucedido, é publicado um `PatientRegisteredEvent` via `RabbitTemplate` para um exchange configurado (`hap.rabbitmq.exchange`).
- `PatientEventHandler` ouve esse evento e atualiza o read model (MongoDB), garantindo que as queries são rápidas e desacopladas do modelo de escrita.
- Outros serviços podem consumir este evento para manter os seus próprios modelos de leitura sincronizados.

**Q3: O CQRS e o AMQP substituem o peer-forwarding?**
- Não. CQRS e AMQP tratam da separação leitura/escrita e da disseminação assíncrona de eventos entre serviços.
- O peer-forwarding continua a ser usado entre instâncias do mesmo serviço (por exemplo em `GET /patients/{id}`) para encontrar dados que ainda não foram replicados localmente.
- Assim, temos dois mecanismos complementares:
  - Eventos AMQP para sincronização entre componentes diferentes (patients, physicians, appointments, auth).
  - Peer-forwarding HTTP para leitura entre instâncias do mesmo componente quando o dado não existe localmente.

**Q4: Estão a usar event sourcing completo ou Sagas no hap-patients?**
- Não. No hap-patients usamos um event log (`PatientEvent`) para auditoria, combinado com CQRS e eventos AMQP, mas o estado oficial do paciente continua na base de dados relacional.
- Não há uma Saga distribuída formal para o registo de pacientes; em vez disso, optou-se por uma integração síncrona simples com o serviço de autenticação e emissão de eventos para integração assíncrona com outros serviços.

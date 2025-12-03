# hap-patients — Gestão de Pacientes

Este serviço gere o registo e a consulta de pacientes e suporta peer-forwarding entre instâncias para leitura distribuída.

## Perfis e Portas
- instance1 → 8082
- instance2 → 8088

## Executar (Windows, cmd.exe)
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
- **Peer-forwarding HTTP entre instâncias:**
  - Continua ativo mesmo com CQRS/AMQP: se a instância local ainda não conhece o paciente, tenta sequencialmente os peers configurados.
  - Implementado no `PatientController` utilizando `ResilientRestTemplate` para lidar com falhas temporárias de peers.
- **Isolamento entre serviços:**
  - Não há imports diretos de classes de outros módulos; a integração é sempre via HTTP/REST ou eventos AMQP.

## Limitações conhecidas
- Sem service discovery e sem circuit breaker.
- Sem cache distribuída; consistência eventual entre instâncias.
- Eventos focados nos cenários principais (por exemplo, `PatientRegisteredEvent`); extensões para outros eventos são possíveis mas não totalmente exploradas aqui.

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
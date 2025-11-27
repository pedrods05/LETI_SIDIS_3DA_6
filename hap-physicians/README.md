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

## CQRS

Na nossa implementação Java com Spring Boot, os conceitos de CQRS foram mapeados da seguinte forma:

- **Os Commands** (ex: `registerPhysician`, `createAppointment`, `updateAppointment`, `cancelAppointment`) são representados pelos métodos transacionais nos serviços `PhysicianCommandService` e `AppointmentCommandService`, que atuam sobre o **Modelo de Escrita (JPA/H2)**.

- **As Queries** (ex: `getPhysicianById`, `getAllAppointments`, `getAppointmentById`, `listUpcomingAppointments`) são representadas pelos métodos de leitura nos serviços `PhysicianQueryService` e `AppointmentQueryService`, que consultam o **Modelo de Leitura (MongoDB)**.

- **Os DTOs de entrada** (`RegisterPhysicianRequest`, `ScheduleAppointmentRequest`, `UpdateAppointmentRequest`) funcionam efetivamente como os objetos de comando.

- **Eventos** são publicados via RabbitMQ após cada operação de escrita bem-sucedida:
  - `PhysicianRegisteredEvent` → atualiza `PhysicianSummary` no MongoDB
  - `AppointmentCreatedEvent` → atualiza `AppointmentSummary` no MongoDB
  - `AppointmentUpdatedEvent` → atualiza `AppointmentSummary` no MongoDB
  - `AppointmentCanceledEvent` → atualiza `AppointmentSummary` no MongoDB

- **Consistência Eventual**: O modelo de leitura (MongoDB) é atualizado de forma assíncrona através de event handlers que consomem eventos do RabbitMQ. Em caso de dados incompletos no modelo de leitura, há um mecanismo de fallback que consulta o modelo de escrita (JPA/H2).

## Colaboração entre serviços (HTTP/REST)
- Patients: GET http://localhost:{8082|8088}/patients/{id}
- Appointment Records: GET http://localhost:{8083|8090}/api/appointment-records/{id}
- Auth: quando aplicável, propagação de Authorization, X-User-Id, X-User-Role

## Peer-forwarding
- Se um recurso não existir na instância local, o serviço tenta o(s) peer(s) usando os endpoints públicos (não usa /internal).

## Configuração (exemplo)
- Bases URL remotas via application.properties (por profile):
  - hap.patients.base-url, hap.appointmentrecords.base-url, hap.auth.base-url

## Swagger
- http://localhost:8081/swagger-ui.html (instância 1)
- http://localhost:8087/swagger-ui.html (instância 2)

## Decisões e Notas
- RestTemplate para simplicidade (bloqueante).
- Headers de segurança propagados entre chamadas.
- Consistência eventual entre instâncias; fallback para peers.

## Limitações conhecidas
- Sem service discovery (peers configurados manualmente).
- Sem circuit breaker/retries com backoff.
- Sem cache distribuída.

## Testes e build
```cmd
mvnw.cmd -q test
mvnw.cmd -q -DskipTests package
```


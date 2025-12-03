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
- Se um paciente não existir localmente, consulta peers pelas mesmas rotas públicas (evita endpoints internos).

## Configuração (exemplo)
- Bases URL remotas via application.properties (por profile):
  - hap.auth.base-url

## Swagger
- http://localhost:8082/swagger-ui.html (instância 1)
- http://localhost:8088/swagger-ui.html (instância 2)

## Decisões e Notas
- RestTemplate / ResilientRestTemplate para simplicidade e isolamento de módulos (sem imports diretos de outros serviços).
- Validações de entrada e gestão de conflitos.
- Peer-forwarding complementa CQRS e AMQP: continua a existir para leitura entre instâncias mesmo após introduzir eventos e modelos de leitura.

## Limitações conhecidas
- Sem service discovery e sem circuit breaker.
- Sem cache distribuída; consistência eventual entre instâncias.

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
- Query (leitura): `GET /patients/{id}` usa `PatientController` + `PatientQueryService`, que lê de `PatientQueryRepository` e devolve um `PatientProfileDTO` sem efeitos de escrita.

**Q2: Onde é que se vê AMQP / message broker?**
- No `PatientRegistrationService`, após um registo bem sucedido, é publicado um `PatientRegisteredEvent` via `RabbitTemplate` para um exchange configurado (`hap.rabbitmq.exchange`).
- Outros serviços podem consumir este evento para manter os seus próprios modelos de leitura sincronizados.

**Q3: O CQRS e o AMQP substituem o peer-forwarding?**
- Não. CQRS e AMQP tratam da separação leitura/escrita e da disseminação assíncrona de eventos entre serviços.
- O peer-forwarding continua a ser usado entre instâncias do mesmo serviço (por exemplo em `GET /patients/{id}/profile`) para encontrar dados que ainda não foram replicados localmente.
- Assim, temos dois mecanismos complementares:
  - Eventos AMQP para sincronização entre componentes diferentes (patients, physicians, appointments, auth).
  - Peer-forwarding HTTP para leitura entre instâncias do mesmo componente quando o dado não existe localmente.
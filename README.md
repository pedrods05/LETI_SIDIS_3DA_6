# HAP — Distributed Healthcare (Physicians, Patients, Auth, Appointment Records)

Este monorepo contém 4 serviços Spring Boot que comunicam por HTTP/REST e suportam execução em 2 instâncias (peers) por serviço.

- hap-physicians — gestão de consultas (futuros, update/cancel, agregação)
- hap-patients — gestão de pacientes (registo e consulta)
- hap-auth — autenticação/registo público
- hap-appointmentrecords — registos de consulta (completed records, source-of-truth)

Requisitos
- JDK 17+ (ou 21+)
- Maven 3.9+

Portas por instância (profiles)
- Physicians: instance1 → 8081, instance2 → 8087
- Patients: instance1 → 8082, instance2 → 8088
- Auth: instance1 → 8084, instance2 → 8089
- Appointment Records: instance1 → 8083, instance2 → 8090

Correr serviços (Windows, cmd.exe)
Abra uma consola por serviço e execute:

```cmd
cd hap-auth
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1

cd hap-patients
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1

cd hap-appointmentrecords
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1

cd hap-physicians
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1
```

Para a segunda instância de cada serviço, substitua o profile por `instance2`.

Testes e build
```cmd
mvnw.cmd -q -DskipITs test
mvnw.cmd -q -DskipTests package
```

Cheat‑sheet de endpoints (linha de base)
- Auth (8084/8089)
  - POST /api/public/login
  - POST /api/public/register
- Patients (8082/8088)
  - GET  /patients/{id}
  - POST /api/v2/patients/register
- Appointment Records (8083/8090)
  - POST /api/appointment-records/{id}/record
  - GET  /api/appointment-records/{id}
- Physicians (8081/8087)
  - POST /physicians/register
  - POST /appointments
  - GET  /physicians/{id}
  - GET  /appointments
  - PUT  /appointments/{id}
  - PUT  /appointments/{id}/cancel
  - GET  /appointments/upcoming

Notas de integração
- As chamadas entre serviços propagam headers (Authorization, X-User-Id, X-User-Role) quando aplicável.
- O Physicians agrega dados remotos (records) e locais (futuros) e faz forward entre peers quando necessário.
- Lista de peers estática por módulo, pré-configurada para localhost nas portas de cada instância.
- Peer forwarding: quando uma instância não tem o recurso localmente, tenta peers do mesmo serviço; pode usar endpoints internos ("/internal/...") conforme o módulo.
- Se nenhuma instância possuir o recurso após tentar todos os peers, o serviço responde 404 Not Found.
Documentação (C4)
- Índice: [DOCS/Docs.md](./DOCS/Docs.md)


## Architecture overview & decisions
- Containers (C2): quatro serviços independentes (Physicians, Patients, Auth, AppointmentRecords) comunicam por HTTP/REST. Cada um pode correr em 2 instâncias (peers) para redundância e distribuição de carga.
- Ownership de dados:
  - Patients: guarda e serve pacientes. Fonte única para dados de paciente.
  - Physicians: cria/gera consultas futuras e faz operações de update/cancel; agrega dados de pacientes e de records quando necessário.
  - AppointmentRecords: guarda registos de consultas concluídas (diagnóstico, prescrições, duração).
  - Auth: autenticação e registo público (sem dependência direta dos outros dados clínicos).
- Colaboração entre serviços:
  - RestTemplate para chamadas HTTP síncronas entre containers; headers de autorização e identidade são propagados.
  - Peer forwarding: quando uma instância não tem o recurso localmente, tenta peers do mesmo serviço; pode usar endpoints internos ("/internal") de cada módulo quando aplicável.
  - Endpoints públicos em Auth para login/register; serviços propagam Authorization/X-User-Id/X-User-Role nas chamadas a outros serviços quando aplicável.
- Resiliência e consistência:
  - Estratégia de fallback simples (tenta primeiro local, depois peers). Consistência eventual entre instâncias.
## Known limitations
- Não há service discovery (lista de peers é estática/configurada por profile).
- RestTemplate (bloqueante) é usado por simplicidade; não há WebClient/Reactor.
- Sem cache distribuída; apenas caches locais e consultas diretas.
- Sem migrations de BD formais (Flyway/Liquibase); dados iniciais são semeados via bootstrap.
- Segurança simplificada; depende de cabeçalhos e validação básica.

## Screenshots (running system)
- Swagger UI (exemplos):
  - Physicians: http://localhost:8081/swagger-ui.html
  - Patients: http://localhost:8082/swagger-ui.html
  - Appointment Records: http://localhost:8083/swagger-ui.html
  - Auth: http://localhost:8084/swagger-ui.html
- Exemplos de capturas de ecrã :
  - [DOCS/screenshots/criarPaciente.png](./DOCS/screenshots/criarPaciente.png)
  - [DOCS/screenshots/loginAdmin.png](./DOCS/screenshots/loginAdmin.png)
  - [DOCS/screenshots/ScheduleAppointment.png](./DOCS/screenshots/ScheduleAppointment.png)

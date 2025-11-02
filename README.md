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

Documentação (C4)
- Índice: DOCS/README.md (ou DOCS/Documentação.md se preferir versão PT)
- C1 — System Context: [PUML](./DOCS/C1/C1-SystemContextDiagram.puml) · [SVG](./DOCS/C1/C1-SystemContextDiagram.svg)
- C2 — Containers: [PUML](./DOCS/C2/C2-Containers.puml) · [SVG](./DOCS/C2/C2-Containers.svg)
- C3 — Logical View: [PUML](./DOCS/C3/C3-LogicalView.puml) · [SVG](./DOCS/C3/C3-LogicalView.svg)
- C4 — Components (placeholder): [PUML](./DOCS/C4/C4-Components.puml)



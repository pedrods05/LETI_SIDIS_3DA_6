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
- RestTemplate para simplicidade e isolamento de módulos (sem imports diretos de outros serviços).
- Validações de entrada e gestão de conflitos.

## Limitações conhecidas
- Sem service discovery e sem circuit breaker.
- Sem cache distribuída; consistência eventual.

## Testes e build
```cmd
mvnw.cmd -q test
mvnw.cmd -q -DskipTests package
```


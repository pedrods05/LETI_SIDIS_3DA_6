# hap-auth — Autenticação e Registo Público

Este serviço expõe endpoints públicos de autenticação e registo de utilizadores e é consumido pelos restantes serviços.

## Perfis e Portas
- instance1 → 8084
- instance2 → 8089

## Executar (Windows, cmd.exe)
```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1
```
Para a segunda instância:
```cmd
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance2
```

## Endpoints principais
- POST /api/public/login
- POST /api/public/register

## Integradores
- Consumido por: hap-patients (registo), hap-physicians e hap-appointmentrecords (validação de identidade/roles quando aplicável).

## Configuração
- JWT/segurança definidos no módulo; endpoints públicos sob /api/public.

## Swagger
- http://localhost:8084/swagger-ui.html (instância 1)
- http://localhost:8089/swagger-ui.html (instância 2)

## Decisões e Notas
- Endpoints públicos para facilitar o registo/login dos utilizadores finais.
- Propagação de headers por serviços clientes quando aplicável.

## Limitações conhecidas
- Sem gestão de sessões distribuídas; foco em token-based auth.

## Testes e build
```cmd
mvnw.cmd -q test
mvnw.cmd -q -DskipTests package
```




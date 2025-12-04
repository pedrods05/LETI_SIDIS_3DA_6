# C4 — Modules Overview

Este documento resume cada módulo do sistema HAP para facilitar a revisão do docente.

- Módulos:
  - hap-physicians
  - hap-patients
  - hap-auth
  - hap-appointmentrecords

Perfis/Portas por instância
- Physicians: instance1 → 8081, instance2 → 8087
- Patients: instance1 → 8082, instance2 → 8088
- Auth: instance1 → 8084, instance2 → 8089
- Appointment Records: instance1 → 8083, instance2 → 8090

---

## hap-physicians

Overview
- Responsável pelo ciclo de vida das consultas futuras (criação, atualização, cancelamento) e pela agregação de dados (consulta + dados do paciente e registos completos quando necessário). Suporta peer-forwarding entre instâncias.

Tecnologias Utilizadas
- Spring Boot 3.5.6, Spring Web, Spring Data JPA, Spring Security, Validation, H2 (dev), Lombok, Maven

Comunicação Inter-Serviços
- HTTP/REST via RestTemplate, sem dependências de código entre módulos.
- Serviços comunicados:
  - hap-patients: obter dados de paciente (GET /patients/{id})
  - hap-appointmentrecords: consultar/cruzar registos concluídos (GET /api/appointment-records/{id}) quando necessário
  - hap-auth: validação de credenciais (quando aplicável, propagate Authorization/X-User-Id/X-User-Role)

Configuração HTTP
- RestTemplate com timeouts e propagação de headers. Bases URL por profile (application.properties).

Exemplo de Uso (lógico)
- Criação de consulta: valida dados, persiste localmente, pode enriquecer com dados via Patients.

Comunicação Pura HTTP/REST
- Não importa classes de outros módulos. Usa DTOs locais.

Endpoints de Comunicação (principais)
- POST /physicians/register
- POST /appointments
- GET  /physicians/{id}
- GET  /appointments
- PUT  /appointments/{id}
- PUT  /appointments/{id}/cancel
- GET  /appointments/upcoming

Estrutura do Projeto (alto nível)
- api/ (controllers), service/, repository/, model/, http/ (clients), config/

Funcionalidades
- Gestão de consultas futuras, atualização/cancelamento, peer-forwarding quando recurso não existe localmente.

Tratamento de Erros
- Exceções traduzidas em ProblemDetails/HTTP adequados; falhas remotas geram mensagens claras.

Testes
- Unitários de serviços e controladores (MockMvc), e testes de peer-forwarding quando aplicável.

Execução
- mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1 (Windows)

---

## hap-patients

Overview
- Gestão de pacientes: registo e consulta de dados de paciente. Suporta peer-forwarding entre instâncias para leitura distribuída.

Tecnologias Utilizadas
- Spring Boot 3.5.6, Spring Web, Spring Data JPA, Spring Security, Validation, H2 (dev), Lombok, Maven

Comunicação Inter-Serviços
- HTTP/REST via RestTemplate.
- Serviços comunicados:
  - hap-auth: registo público (POST /api/public/register) e, quando necessário, validação (Authorization)

Configuração HTTP
- RestTemplate com timeouts; base URL do Auth via properties.

Exemplo de Uso (lógico)
- Registo de paciente chama Auth para criar credenciais públicas e persiste dados locais do paciente.

Comunicação Pura HTTP/REST
- Não importa classes de Auth/Physicians; usa DTOs locais e HTTP.

Endpoints de Comunicação (principais)
- GET  /patients/{id}
- POST /api/v2/patients/register

Estrutura do Projeto (alto nível)
- api/, service/, repository/, model/, http/, config/

Funcionalidades
- Registar e consultar pacientes, com validação e tratamento de conflitos.

Tratamento de Erros
- Erros de validação (400), não encontrado (404), conflitos (409) e falhas de comunicação externa.

Testes
- Unitários de serviço (mocks de RestTemplate) e controladores com MockMvc.

Execução
- mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1

---

## hap-auth

Overview
- Autenticação e registo público (sem dependência direta de dados clínicos). Emite/valida tokens e expõe endpoints públicos.

Tecnologias Utilizadas
- Spring Boot 3.5.6, Spring Web, Spring Security, Validation, H2 (dev), Lombok, Maven

Comunicação Inter-Serviços
- Normalmente é chamado pelos outros serviços (não chama outros).

Configuração HTTP
- Endpoints públicos expostos sob /api/public.

Exemplo de Uso (lógico)
- Registo/Login de utilizadores; resposta com token/identidade.

Comunicação Pura HTTP/REST
- Consumido via RestTemplate pelos outros módulos.

Endpoints de Comunicação (principais)
- POST /api/public/login
- POST /api/public/register

Estrutura do Projeto (alto nível)
- api/, service/, repository/, model/, config/

Funcionalidades
- Gestão de credenciais básicas e emissão/validação de tokens.

Tratamento de Erros
- Credenciais inválidas (401/403), conflitos de registo (409), validações (400).

Testes
- Unitários e de controlador.

Execução
- mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1

---

## hap-appointmentrecords

Overview
- Gestão de registos de consultas concluídas (diagnóstico, recomendações, prescrições, duração). Fonte de verdade para records. Pode consultar Physicians/Pacients por HTTP.

Tecnologias Utilizadas
- Spring Boot 3.5.6, Spring Web, Spring Data JPA, Spring Security, Validation, H2 (dev), Lombok, Maven

Comunicação Inter-Serviços
- HTTP/REST via RestTemplate.
- Serviços comunicados:
  - hap-physicians: obter dados da consulta (GET /appointments/{id})
  - hap-patients: obter dados do paciente (GET /patients/{id})
  - hap-auth: validação (Authorization) quando aplicável

Configuração HTTP
- RestTemplate com timeouts e propagação de headers; bases URL em application.properties por profile.

Exemplo de Uso (lógico)
- Criar record: valida autorização do médico para a consulta, busca consulta por HTTP, persiste record.

Comunicação Pura HTTP/REST
- Não importa classes de outros módulos; apenas DTOs locais e HTTP.

Endpoints de Comunicação (principais)
- POST /api/appointment-records/{id}/record
- GET  /api/appointment-records/{id}

Estrutura do Projeto (alto nível)
- api/, service/, repository/, model/, http/, config/, exceptions/

Funcionalidades
- Criação e consulta de registos; integração com serviços externos para dados complementares.

Tratamento de Erros
- Comunicação externa (timeouts, 4xx/5xx), validações, não encontrado.

Testes
- Unitários de serviço, controladores com MockMvc.

Execução
- mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1

---

Notas finais
- Cada módulo possui o seu próprio README para detalhes específicos (setup, endpoints, decisões locais). Este documento central no DOCS/C4 oferece uma visão conjunta para avaliação e referência rápida.


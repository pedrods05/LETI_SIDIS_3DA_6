# Documentação de Testes - Módulo hap-physicians

## Visão Geral
Este documento fornece documentação dos principais testes unitários e de integração no módulo hap-physicians, organizados por funcionalidade e padrões arquiteturais.

**Última Atualização:** 8 de Dezembro de 2025  
**Total de Classes de Teste:** 12  
**Áreas de Cobertura de Testes:** API Controllers, Services, Repositories, Events, Configuration, Models, Utilities, Integration

---

## Índice
1. [Testes da Camada API](#testes-da-camada-api)
2. [Testes da Camada de Serviços](#testes-da-camada-de-serviços)
3. [Testes de Cliente HTTP](#testes-de-cliente-http)
4. [Testes de Model / DTO](#testes-de-model--dto)
5. [Testes de Utilitários](#testes-de-utilitários)
6. [Testes de Tratamento de Exceções](#testes-de-tratamento-de-exceções)
7. [Executar os Testes](#executar-os-testes)

---

## Testes da Camada API

### 1. PhysicianControllerTest
**Ficheiro:** `src/test/java/leti_sisdis_6/happhysicians/api/PhysicianControllerTest.java`

**Propósito:** Testa os principais endpoints de médicos expostos a consumidores externos.

**Cenários Típicos Cobertos:**
- Recuperação bem-sucedida de um médico por id
- Registo bem-sucedido de um novo médico
- Atualização bem-sucedida de informações do médico
- Cálculo e recuperação de slots de consulta disponíveis
- Comportamento quando o médico não existe (404)
- Estrutura de resposta (campos DTO) e códigos de estado HTTP para caminhos de sucesso e erro
- Integração com serviços de comando e query (padrão CQRS)

**Preocupações Principais:**
- Códigos de estado HTTP corretos (200, 201, 404, 5xx)
- Delegação para a camada de serviço / comando / query em vez de duplicar lógica de negócio
- Uso adequado do padrão CQRS (comandos para escrita, queries para leitura)

---

### 2. AppointmentControllerTest
**Ficheiro:** `src/test/java/leti_sisdis_6/happhysicians/api/AppointmentControllerTest.java`

**Propósito:** Testa endpoints HTTP responsáveis pela gestão de consultas (agendamento, atualização, cancelamento, recuperação).

**Cenários Típicos Cobertos:**
- Criação bem-sucedida de uma consulta
- Recuperação bem-sucedida de detalhes de consulta
- Atualização bem-sucedida de informações de consulta
- Cancelamento bem-sucedido de uma consulta
- Recuperação do trilho de auditoria de uma consulta (Event Sourcing)
- Erros de validação (campos obrigatórios em falta, formatos inválidos) e respostas 400 resultantes
- Interação com serviços de comando e query (padrão CQRS)
- Integração com EventStoreService para funcionalidade de trilho de auditoria

**Preocupações Principais:**
- Códigos de estado HTTP corretos (200, 201, 400, 404, 5xx)
- Uso adequado do padrão CQRS (comandos para escrita, queries para leitura)
- Integração de event sourcing para funcionalidade de trilho de auditoria

---

## Testes da Camada de Serviços

### 3. PhysicianServiceTest
**Ficheiro:** `src/test/java/leti_sisdis_6/happhysicians/services/PhysicianServiceTest.java`

**Propósito:** Testa a lógica de negócio principal dos médicos, incluindo registo, recuperação e atualizações.

**Cenários Típicos Cobertos:**
- Obter detalhes do médico quando existem
- Registar um novo médico com dados válidos
- Atualizar informações do médico
- Pesquisar médicos por nome ou especialidade
- Comportamento quando o médico não existe
- Integração com repositórios e serviços externos

---

### 4. AppointmentServiceTest
**Ficheiro:** `src/test/java/leti_sisdis_6/happhysicians/services/AppointmentServiceTest.java`

**Propósito:** Testa a lógica de negócio das consultas, incluindo agendamento, validação e integração com outros serviços.

**Cenários Típicos Cobertos:**
- Fluxo de criação bem-sucedida de consulta com validações
- Recuperação de consulta com detalhes de paciente e registo
- Atualizações e cancelamentos de consultas
- Tratamento de conflitos de slots de tempo
- Integração com serviços externos (pacientes, registos de consultas)
- Validação de slots de tempo de consulta contra horários de trabalho do médico
- Tratamento de transições de estado de consulta

**Preocupações Principais:**
- Validação de regras de negócio (slots de tempo, disponibilidade do médico)
- Integração com ExternalServiceClient para comunicação entre serviços
- Tratamento adequado de erros para falhas de serviços externos

---

### 5. ExternalServiceClientTest
**Ficheiro:** `src/test/java/leti_sisdis_6/happhysicians/services/ExternalServiceClientTest.java`

**Propósito:** Testa o cliente HTTP usado para comunicação entre serviços com hap-patients, hap-auth e hap-appointmentrecords.

**Cenários Típicos Cobertos:**
- Recuperação bem-sucedida de dados de pacientes do hap-patients
- Recuperação bem-sucedida de registos de consultas do hap-appointmentrecords
- Registo e validação de utilizadores com hap-auth
- Configuração e recuperação de URLs de peers
- Tratamento de erros para falhas de serviços externos
- Comportamento do circuit breaker (quando aplicável)

**Preocupações Principais:**
- Chamadas HTTP corretas para serviços externos
- Tratamento adequado de erros e tradução de exceções
- Padrões de resiliência (circuit breaker, retries)

---

## Testes de Cliente HTTP

### 6. ResilientRestTemplateTest
**Ficheiro:** `src/test/java/leti_sisdis_6/happhysicians/http/ResilientRestTemplateTest.java`

**Propósito:** Testa a configuração do cliente HTTP / RestTemplate para chamadas entre serviços.

**Cenários Típicos Cobertos:**
- Presença de interceptores para adicionar headers (ex: token de autenticação, correlation ID)
- Configuração básica de timeout / tratamento de erros
- Lógica de retry para falhas transitórias

---

## Testes de Model / DTO

### 7. AppointmentDetailsDTOTest
**Ficheiro:** `src/test/java/leti_sisdis_6/happhysicians/dto/output/AppointmentDetailsDTOTest.java`

**Propósito:** Testa o DTO usado para retornar detalhes de consultas em respostas da API.

**Cenários Típicos Cobertos:**
- Criação de objetos DTO com todos os campos relevantes
- Comportamento de getter/setter / Lombok
- Mapeamento de entidades de domínio para DTOs

---

### 8. ConsultationTypeTest
**Ficheiro:** `src/test/java/leti_sisdis_6/happhysicians/model/ConsultationTypeTest.java`

**Propósito:** Testa o enum ConsultationType usado para categorizar consultas.

**Cenários Típicos Cobertos:**
- Validação de valores do enum
- Conversão e parsing de strings

---

### 9. AppointmentStatusTest
**Ficheiro:** `src/test/java/leti_sisdis_6/happhysicians/model/AppointmentStatusTest.java`

**Propósito:** Testa o enum AppointmentStatus usado para rastrear o ciclo de vida das consultas.

**Cenários Típicos Cobertos:**
- Validação de valores do enum
- Validação de transições de estado
- Conversão e parsing de strings

---

## Testes de Utilitários

### 10. AppointmentTimeValidatorTest
**Ficheiro:** `src/test/java/leti_sisdis_6/happhysicians/util/AppointmentTimeValidatorTest.java`

**Propósito:** Testa a classe utilitária responsável por validar slots de tempo de consultas.

**Cenários Típicos Cobertos:**
- Validação de slots de tempo dentro dos horários de trabalho do médico
- Deteção de consultas sobrepostas
- Validação de tempos de consulta no futuro
- Tratamento de casos extremos (tempos limite, considerações de timezone)

---

### 11. SlotCalculatorTest
**Ficheiro:** `src/test/java/leti_sisdis_6/happhysicians/util/SlotCalculatorTest.java`

**Propósito:** Testa a classe utilitária responsável por calcular slots de consulta disponíveis.

**Cenários Típicos Cobertos:**
- Cálculo de slots disponíveis com base nos horários de trabalho do médico
- Exclusão de slots de tempo já reservados
- Tratamento de diferentes durações de slots
- Casos extremos (sem slots disponíveis, todos os slots disponíveis)

---

## Testes de Tratamento de Exceções

### 12. GlobalExceptionHandlerTest
**Ficheiro:** `src/test/java/leti_sisdis_6/happhysicians/exceptions/GlobalExceptionHandlerTest.java`

**Propósito:** Testa o `@ControllerAdvice` que centraliza o tratamento de erros para o módulo.

**Cenários Típicos Cobertos:**
- Erros de validação traduzidos para HTTP 400 com informações de erro estruturadas
- Cenários de não encontrado mapeados para 404
- Cenários de conflito mapeados para 409
- Erros de comunicação com serviços externos mapeados para códigos de estado apropriados
- Tratamento genérico de exceções para erros inesperados

---

## Executar os Testes

A partir da raiz do módulo `hap-physicians` pode executar todos os testes com Maven:

```bash
mvn test
```

### Executar Testes no IntelliJ
- Clique com o botão direito em `src/test/java` → **Run 'All Tests'**
- Clique com o botão direito numa classe de teste específica → **Run 'ClassNameTest'**
- Use `Ctrl+Shift+F10` para executar o teste no cursor

### Executar Classes de Teste Específicas
```bash
mvn test -Dtest=PhysicianControllerTest
mvn test -Dtest=AppointmentServiceTest
mvn test -Dtest=ExternalServiceClientTest
```

---

# Deployment, CI/CD, and Governance

Este documento explica as práticas de **deployment**, **CI/CD** e **governance** implementadas no `hap-physicians` para garantir deploy contínuo, descoberta de serviços, contratos de API e rastreabilidade através de Event Sourcing.

## Containerização com Docker

### O que é e por que é importante?

Containerização permite empacotar a aplicação e todas as suas dependências num container isolado, garantindo que funcione da mesma forma em qualquer ambiente (desenvolvimento, teste, produção).

### O que está implementado:

**Dockerfile Multi-Stage:**

O módulo utiliza um Dockerfile multi-stage que otimiza o tamanho da imagem final:

1. **Estágio de Build:**
    - Usa `maven:3.8-openjdk-17` para compilar o código
    - Aproveita cache de dependências (copia `pom.xml` primeiro)
    - Executa `mvn clean package -DskipTests` (testes correm no CI/CD antes)

2. **Estágio de Runtime:**
    - Usa `eclipse-temurin:17-jdk-alpine` (imagem leve)
    - Cria utilizador não-root (`hapuser`) para segurança
    - Copia apenas o JAR compilado do estágio anterior
    - Expõe porta 8080

**Benefício:** Imagem final é mais leve (apenas runtime, sem ferramentas de build) e mais segura (utilizador não-root).

**Em termos simples:** É como ter uma caixa que contém tudo o que a aplicação precisa para funcionar. Esta caixa funciona da mesma forma em qualquer lugar, seja no computador do desenvolvedor ou num servidor de produção.

---

## Service Catalog (Backstage)

### O que é e por que é importante?

Service Catalog permite descoberta e documentação de serviços. O Backstage é uma plataforma open-source da Spotify que ajuda equipas a descobrir, entender e consumir serviços.

### O que está implementado:

**catalog-info.yaml:**

O módulo tem um ficheiro `catalog-info.yaml` que registra o serviço no Backstage:

- **Nome:** `hap-physicians`
- **Tipo:** `service`
- **Descrição:** Gestão de médicos, consultas e Event Sourcing com audit trail
- **Lifecycle:** `production`
- **Owner:** `hap-team`
- **System:** `hap-system`
- **APIs fornecidas:** `hap-physicians-api`
- **Anotações:** Configuração para Prometheus scraping

**Benefício:** Permite que outras equipas descubram e entendam o serviço, facilitando integração e colaboração.

**Em termos simples:** É como ter um catálogo de serviços onde qualquer pessoa pode procurar e encontrar informações sobre o `hap-physicians`, saber quem é responsável, que APIs fornece, etc.

---

## API Contracts (OpenAPI e Pact)

### O que são e por que são importantes?

Contratos de API definem como a API deve funcionar. Isto garante que consumidores e fornecedores estejam alinhados sobre o que a API faz e como deve ser usada.

### OpenAPI/Swagger

**O que está implementado:**

- **OpenAPI 3.0** configurado via `springdoc-openapi`
- **Swagger UI** disponível em `/swagger-ui.html`
- **Especificação OpenAPI** disponível em `/v3/api-docs`
- **Configuração de segurança:** JWT Bearer token configurado no Swagger
- **Documentação automática:** Todos os endpoints são documentados automaticamente

**Endpoints disponíveis:**
- `http://localhost:8081/swagger-ui.html` (instância 1)
- `http://localhost:8087/swagger-ui.html` (instância 2)

**Benefício:** Desenvolvedores podem ver e testar a API diretamente no browser, facilitando integração e reduzindo erros.

**Em termos simples:** É como ter um manual interativo da API. Qualquer pessoa pode abrir o Swagger, ver todos os endpoints disponíveis, que parâmetros precisam, e até testar a API diretamente.

### Contract Testing com Pact

**O que é:** Contract Testing garante que o contrato entre serviços (consumer e provider) seja respeitado. O Pact permite testar contratos sem precisar de ambos os serviços rodando simultaneamente.

**O que está implementado:**

- **Pact Provider Tests** implementados em `PactProviderTest.java`
- **Validação de contratos:** O serviço valida que cumpre os contratos definidos pelos consumidores (ex: `hap-patients`)
- **Pact Folder:** Contratos são carregados de `../hap-patients/target/pacts`
- **Provider Name:** `hap-physicians-service`

**Como funciona:**
1. Consumidores (ex: `hap-patients`) geram contratos Pact quando testam
2. Estes contratos são armazenados em ficheiros
3. O `hap-physicians` (provider) valida que cumpre estes contratos nos seus testes
4. Se o contrato for quebrado, os testes falham

**Benefício:** Detecta quebras de contrato antes de chegar a produção, prevenindo que mudanças numa API quebrem consumidores.

**Em termos simples:** É como ter um acordo escrito entre dois serviços. Se um serviço mudar e quebrar o acordo, os testes detectam imediatamente, antes que cause problemas em produção.

---

## SLAs (Service Level Agreements)

### O que são e por que são importantes?

SLAs definem métricas de performance que o serviço deve cumprir. Isto garante que o serviço seja responsivo e confiável.

### O que está implementado:

**SLA Configurado:**

- **99% dos pedidos devem ser respondidos em menos de 2 segundos**
- **Timeout agressivo:** 200ms para operações críticas com SLA
- **Circuit Breaker:** Falha rapidamente se não conseguir cumprir o SLA

**Monitorização:**
- Métricas expostas em `/actuator/prometheus`
- Permite configurar alertas no Grafana quando SLA não é cumprido

**Benefício:** Garante que o serviço seja responsivo e permite detectar problemas de performance antes que afetem utilizadores.

**Em termos simples:** É como ter uma promessa de que o serviço vai responder rapidamente. Se não conseguir cumprir, o sistema detecta e pode tomar ações (ex: abrir circuit breaker, alertar).

---

## Event Sourcing

### O que é e por que é importante?

Event Sourcing armazena todas as mudanças de estado como uma sequência de eventos imutáveis (append-only). Em vez de atualizar o estado atual, adiciona-se um novo evento que representa a mudança.

### O que está implementado:

**Event Store:**

- **Tabela `event_store`** - Armazena todos os eventos relacionados a consultas (appointments)
- **Append-only:** Eventos são apenas adicionados, nunca modificados ou apagados
- **Imutável:** Uma vez escrito, um evento não pode ser alterado

**Estrutura de Eventos:**

Cada evento contém:
- **aggregateId:** ID da consulta (appointmentId)
- **eventType:** Tipo do evento (CONSULTATION_SCHEDULED, NOTE_ADDED, etc.)
- **timestamp:** Quando o evento ocorreu
- **eventData:** Dados completos do evento em JSON
- **aggregateVersion:** Versão do agregado (para optimistic locking)
- **correlationId:** ID de correlação para rastreamento
- **userId:** Quem causou o evento
- **metadata:** Metadados adicionais em JSON

**Eventos Registados:**

- `CONSULTATION_SCHEDULED` - Quando uma consulta é criada
- `CONSULTATION_UPDATED` - Quando uma consulta é atualizada
- `CONSULTATION_CANCELED` - Quando uma consulta é cancelada
- `NOTE_ADDED` - Quando uma nota é adicionada
- `CONSULTATION_COMPLETED` - Quando uma consulta é concluída

**Audit Trail:**

- **Endpoint:** `GET /appointments/{id}/audit-trail`
- Retorna histórico completo de todos os eventos de uma consulta
- Permite reconstruir o estado completo em qualquer ponto no tempo
- Protegido: Apenas ADMIN ou PHYSICIAN podem acessar

**Benefício:** Fornece rastreabilidade completa e imutável de todas as mudanças, essencial para auditoria e conformidade (GDPR). Permite também reconstruir o estado histórico e debugar problemas.

**Em termos simples:** É como ter um livro de registos onde todas as ações são escritas com data, hora e quem fez. Este livro não pode ser alterado ou apagado, garantindo um histórico completo e confiável. Se precisar de saber o que aconteceu há 6 meses, pode reconstruir o estado exato daquela altura.

---

## Integração com Saga Pattern

### O que é Saga Pattern?

Saga Pattern é um padrão para gerir transações que envolvem múltiplos serviços. Como não podemos usar transações ACID tradicionais em sistemas distribuídos, o Saga Pattern quebra uma transação complexa em múltiplos passos. Se algo falhar, executamos compensações (rollback) para desfazer o que foi feito.

### Como Event Sourcing se Integra com Saga:

**Audit Trail para Transações Distribuídas:**

- Cada passo de uma Saga gera eventos no Event Store
- O `correlationId` permite rastrear todos os eventos relacionados a uma única transação distribuída
- Permite reconstruir o histórico completo de uma transação que envolveu múltiplos serviços

**Resiliência em Transações:**

- Event Sourcing funciona como trilho de auditoria para operações de Saga
- Se uma Saga falhar e precisar de compensação, o Event Store registra todos os passos e compensações
- Permite investigar falhas em transações distribuídas complexas

**Exemplo:**

Criar uma consulta pode envolver:
1. Validar paciente (chamada a `hap-patients`) → Evento registado
2. Validar médico (verificação local) → Evento registado
3. Criar registo da consulta → Evento `CONSULTATION_SCHEDULED`
4. Publicar evento AMQP → Evento registado

Se qualquer passo falhar, os eventos anteriores permitem rastrear o que foi feito e executar compensações adequadas.

**Benefício:** Combina rastreabilidade completa (Event Sourcing) com resiliência a falhas (Saga Pattern), garantindo que transações distribuídas sejam confiáveis e auditáveis.

---

## CI/CD

### O que é e por que é importante?

CI/CD (Continuous Integration/Continuous Deployment) automatiza testes, build e deployment, garantindo que mudanças sejam testadas e deployadas de forma confiável.

### O que está implementado:

**Preparação para CI/CD:**

- **Dockerfile otimizado:** Multi-stage build que separa compilação de runtime
- **Testes separados:** Dockerfile executa `-DskipTests` porque testes correm no CI/CD antes do build da imagem
- **Utilizador não-root:** Imagem preparada para segurança em produção

**Fluxo esperado:**

1. **CI (Continuous Integration):**
    - Código é commitado
    - Testes unitários e de integração são executados
    - Contract tests (Pact) são executados
    - Se todos os testes passarem, build da imagem Docker

2. **CD (Continuous Deployment):**
    - Imagem Docker é construída e publicada
    - Deployment para ambiente de teste/staging
    - Se bem-sucedido, deployment para produção

**Benefício:** Garante que apenas código testado e validado seja deployado, reduzindo riscos e acelerando o ciclo de desenvolvimento.

**Em termos simples:** É como ter um assistente que automaticamente testa o código, constrói a aplicação e a coloca em produção, mas só se tudo estiver correto. Isto previne que código com problemas chegue aos utilizadores.

---

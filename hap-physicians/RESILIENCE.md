# Resilience and Fault Tolerance

Este documento explica os **padrões de resiliência** implementados no `hap-physicians` utilizando **Resilience4j**, garantindo que o sistema degrade graciosamente sob carga ou problemas de rede.

## Por que Resiliência é Importante?

Em sistemas distribuídos, quando um serviço falha, pode causar uma reação em cadeia que derruba todo o sistema. Os padrões de resiliência previnem isso, garantindo que o sistema continue a funcionar mesmo quando alguns componentes estão indisponíveis.

**Em termos simples:** É como ter sistemas de segurança num edifício. Se uma porta falhar, outras continuam a proteger. O sistema adapta-se e continua a funcionar, mesmo que com funcionalidades reduzidas.

---

## Padrões Implementados

### 1. Circuit Breaker (Disjuntor)

**O que é:** Funciona como um disjuntor elétrico. Monitora chamadas a serviços externos e, quando detecta muitas falhas, "abre o circuito" e para de fazer chamadas, evitando sobrecarregar serviços com problemas.

**Estados:**
- **CLOSED:** Operações normais
- **OPEN:** Bloqueia chamadas após exceder threshold (ex: 50% de falhas)
- **HALF_OPEN:** Após período de espera, permite tentativas para verificar recuperação

**Implementações no módulo:**

1. **`authService`** - Protege chamadas ao serviço de autenticação:
    - Abre se mais de 50% das últimas 10 chamadas falharem
    - Espera 5 segundos antes de tentar novamente
    - Exposto como health indicator

2. **`appointmentRecordsService`** - Protege chamadas ao serviço de registos:
    - Abre se mais de 50% das últimas 10 chamadas falharem
    - Espera 10 segundos antes de tentar novamente

3. **`sagaAmqpPublisher`** - Protege publicações de eventos AMQP:
    - Crítico para transações distribuídas
    - Se eventos não forem publicados, transações podem ficar inconsistentes

**Onde é usado:**
- Chamadas HTTP a `hap-auth` e `hap-appointmentrecords`
- Publicações de eventos AMQP em fluxos de Saga

**Em termos simples:** É como um segurança que, quando vê problemas, para de enviar pessoas para lá e tenta novamente depois. Isto previne que o problema se espalhe.

---

### 2. Retry (Tentativa Novamente)

**O que é:** Tenta novamente automaticamente quando uma operação falha, usando exponential backoff (aumenta o tempo de espera a cada tentativa).

**Implementação no módulo:**

- **`sagaRetry`** - Configurado para operações de Saga e publicações AMQP:
    - Máximo de 3 tentativas
    - Tempo inicial: 500ms
    - Exponential backoff: multiplicador de 2 (500ms → 1000ms)

**Onde é usado:**
- Publicações de eventos AMQP que falharam temporariamente
- Operações de Saga que falharam temporariamente

**Em termos simples:** É como tentar ligar novamente antes de desistir. Tenta imediatamente, depois após 500ms, e por fim após 1000ms. Só depois de 3 tentativas é que desiste.

---

### 3. Timeout (Tempo Limite)

**O que é:** Define tempo máximo de execução para operações, evitando bloqueios indefinidos.

**Implementações no módulo:**

1. **`sagaOperation`** - Timeout de 30 segundos para operações de Saga completas
2. **`standardSla`** - Timeout de 200ms para operações críticas com SLA
3. **Chamadas HTTP** - Timeout de 5 segundos para conexão e requisição

**Onde é usado:**
- Operações de Saga completas (criação de consultas)
- Chamadas HTTPS a serviços externos

**Em termos simples:** É como ter um limite de tempo numa chamada. Se não responder em tempo útil, desliga e tenta outra coisa.

---

### 4. Bulkhead (Compartimento Estanque)

**O que é:** Isola recursos (threads, conexões) para garantir que problemas numa área não afetem outras partes do sistema. O nome vem da arquitetura naval - compartimentos estanques previnem que um vazamento afunde todo o navio.

**Implementação no módulo:**

- **`compensation`** - Pool separado para operações de compensação (rollback):
    - Máximo de 10 compensações simultâneas
    - Se o pool estiver cheio, a operação falha imediatamente

**Onde é usado:**
- Operações de compensação de transações Saga

**Em termos simples:** É como ter salas separadas num hospital. Se houver problemas numa sala, isso não afeta outras onde operações normais continuam. É uma forma de "quarentena" - problemas ficam isolados.

---

## Integração com Saga Pattern

**O que é Saga Pattern:** Padrão para gerir transações que envolvem múltiplos serviços. Quebra uma transação complexa em múltiplos passos. Se algo falhar, executamos compensações (rollback) para desfazer o que foi feito.

**Exemplo:** Criar uma consulta envolve validar paciente, validar médico, criar registo e publicar evento. Se qualquer passo falhar, os anteriores são desfeitos.

### Como os Padrões de Resiliência se Integram:

**Publicações AMQP com Resiliência:**
- Todas as publicações de eventos AMQP têm Circuit Breaker e Retry
- Eventos protegidos: `AppointmentCreatedEvent`, `AppointmentUpdatedEvent`, `AppointmentCanceledEvent`, `AppointmentReminderEvent`
- **Por que é crítico?** Se um evento não for publicado, outros serviços não saberão que uma operação aconteceu, causando inconsistências.

**Timeout em Operações de Saga:**
- Operações de Saga completas têm timeout de 30 segundos
- Previne que transações distribuídas fiquem indefinidamente pendentes

**Isolamento de Compensações:**
- Compensações são isoladas com Bulkhead
- Garante que problemas em rollbacks não afetem operações normais

**Métricas de Saga:**
- `saga.step.duration` - Duração de cada passo
- `saga.compensation.count` - Número de compensações
- `amqp.messages.published` - Mensagens publicadas
- `amqp.messages.failed` - Mensagens que falharam

---

## Monitoramento

**Health Checks:**
- Circuit Breakers expostos como health indicators
- Endpoints: `/actuator/health` e `/actuator/circuitbreakers`
- Permite que orquestradores (Kubernetes) reajam automaticamente quando serviços estão em falha

**Métricas Prometheus:**
- Expostas em `/actuator/prometheus`
- `resilience4j.circuitbreaker.state` - Circuit Breakers em estado OPEN
- `amqp.messages.failure.rate` - Taxa de falhas em mensagens AMQP
- `saga.compensation.count` - Contador de compensações

**Benefício:** Permite detectar e resolver problemas antes que afetem os utilizadores.

---

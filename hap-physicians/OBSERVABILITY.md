# Observability Integration - Implementação Completa

Este documento explica as funcionalidades de **observabilidade** e **resiliência** implementadas no projeto `hap-physicians` para monitorar e proteger o sistema contra falhas.

## 📋 O que é Observabilidade?

Observabilidade permite entender o que está acontecendo dentro do sistema através de três pilares: **logs**, **métricas** e **tracing**. Isso ajuda a identificar problemas rapidamente e garantir que o sistema funcione corretamente.

## ✅ O que foi Implementado

### 1. Logging Estruturado (Pronto para ELK Stack)

**O que faz:** Registra todas as ações do sistema de forma organizada.

**Como funciona:**
- Cada requisição recebe um ID único (Correlation ID) que permite rastrear a mesma operação em diferentes partes do sistema
- Os logs incluem informações de rastreamento (Trace IDs) que conectam diferentes serviços
- Os logs estão formatados de forma estruturada, prontos para serem enviados para ferramentas como ELK Stack (Elasticsearch, Logstash, Kibana) ou Fluentd

**Benefício:** Facilita encontrar problemas e entender o fluxo completo de uma operação através de múltiplos serviços.

### 2. Métricas Customizadas (Prometheus + Grafana)

**O que faz:** Coleta números sobre o desempenho do sistema (quanto tempo leva, quantas vezes acontece, etc.).

**Métricas implementadas:**
- **Duração de operações de Saga:** Mede quanto tempo leva cada passo de uma transação distribuída
- **Compensações:** Conta quantas vezes o sistema teve que desfazer operações
- **Mensagens publicadas:** Conta quantos eventos foram enviados para a fila de mensagens
- **Mensagens consumidas:** Conta quantos eventos foram processados
- **Falhas:** Conta quantas vezes algo deu errado
- **Estados de Circuit Breaker:** Monitora quando proteções automáticas são ativadas

**Benefício:** Permite criar gráficos e alertas no Grafana para visualizar a saúde do sistema em tempo real.

### 3. Distributed Tracing (Zipkin)

**O que faz:** Rastreia uma requisição desde o início até o fim, passando por todos os serviços.

**Como funciona:**
- Quando uma requisição chega, recebe um ID de rastreamento
- Esse ID é propagado automaticamente para outros serviços e filas de mensagens
- No Zipkin, é possível ver o caminho completo: Requisição HTTP → Publicação na fila → Consumo da mensagem → Escrita no banco de dados

**Benefício:** Facilita depurar problemas complexos que envolvem múltiplos serviços.

### 4. Health Checks (Liveness e Readiness)

**O que faz:** Verifica se o sistema está funcionando e pronto para receber tráfego.

**Tipos de verificação:**
- **Liveness:** Verifica se a aplicação está viva (se não estiver, o Kubernetes reinicia automaticamente)
- **Readiness:** Verifica se a aplicação está pronta para receber requisições (se não estiver, remove do balanceador de carga)

**Benefício:** Garante que apenas instâncias saudáveis recebam tráfego e que instâncias com problemas sejam reiniciadas automaticamente.

### 5. Resilience Patterns (Padrões de Resiliência)

**O que faz:** Protege o sistema contra falhas, garantindo que continue funcionando mesmo quando partes dele estão com problemas.

#### Circuit Breaker (Disjuntor)
**O que é:** Como um disjuntor elétrico, interrompe chamadas para serviços que estão falhando repetidamente.

**Como funciona:**
- Monitora falhas em chamadas para outros serviços ou filas de mensagens
- Quando detecta muitas falhas, "abre o circuito" e para de tentar
- Após um tempo, tenta novamente para ver se o serviço voltou
- Enquanto o circuito está aberto, usa métodos alternativos (fallback) para não quebrar o sistema

**Benefício:** Previne que um serviço com problemas cause uma cascata de falhas em todo o sistema.

#### Retry (Tentativa Novamente)
**O que é:** Tenta novamente automaticamente quando uma operação falha.

**Como funciona:**
- Se uma chamada falhar, espera um pouco e tenta novamente
- Aumenta o tempo de espera a cada tentativa (exponential backoff)
- Tenta até 3 vezes antes de desistir

**Benefício:** Lida com falhas temporárias de rede ou serviços que estão momentaneamente indisponíveis.

#### Timeout (Tempo Limite)
**O que é:** Define um tempo máximo para operações, evitando que fiquem travadas indefinidamente.

**Como funciona:**
- Operações HTTP têm timeout de 5-15 segundos
- Operações de Saga têm timeout de 30 segundos
- Se exceder o tempo, a operação é cancelada

**Benefício:** Previne que recursos fiquem presos esperando respostas que nunca virão.

#### Bulkhead (Compartimento Estanque)
**O que é:** Isola recursos para que problemas em uma área não afetem outras.

**Como funciona:**
- Operações de compensação (desfazer ações) rodam em um pool de threads separado
- Se houver muitos problemas em compensações, isso não afeta outras operações normais

**Benefício:** Garante que uma parte problemática do sistema não consuma todos os recursos disponíveis.

### 6. Integração com Saga Pattern

**O que é Saga Pattern:** Um padrão para gerenciar transações que envolvem múltiplos serviços.

**Como está integrado:**
- Todas as publicações de eventos na fila de mensagens têm Circuit Breaker e Retry
- Operações de Saga têm timeout para não ficarem travadas
- Compensações (desfazer operações) são isoladas com Bulkhead
- Métricas rastreiam o desempenho e falhas de todas as operações de Saga

**Benefício:** Garante que transações distribuídas sejam confiáveis e possam ser monitoradas.

### 7. Alertas e Monitoramento

**O que faz:** Detecta problemas automaticamente e pode enviar alertas.

**Como funciona:**
- Health checks detectam quando Circuit Breakers estão abertos
- Métricas permitem configurar alertas no Grafana (ex: "alerta se mais de 10% das mensagens falharem")
- Sistema registra todas as falhas para análise posterior

**Benefício:** Permite detectar e resolver problemas antes que afetem os usuários.

## 🎯 Endpoints Disponíveis

- **Health**: `http://localhost:8081/actuator/health` - Status geral do sistema
- **Liveness**: `http://localhost:8081/actuator/health/liveness` - Verifica se está vivo
- **Readiness**: `http://localhost:8081/actuator/health/readiness` - Verifica se está pronto
- **Metrics**: `http://localhost:8081/actuator/metrics` - Lista todas as métricas
- **Prometheus**: `http://localhost:8081/actuator/prometheus` - Métricas no formato Prometheus
- **Circuit Breakers**: `http://localhost:8081/actuator/circuitbreakers` - Status dos disjuntores
- **Zipkin**: `http://localhost:9411` - Interface para visualizar traces

## 📊 O que é Monitorado

**Operações de Saga (Transações Distribuídas):**
- Quanto tempo cada passo leva
- Quantas vezes compensações foram necessárias
- Fluxo completo de uma transação através de múltiplos serviços

**Mensagens AMQP (Fila de Eventos):**
- Quantas mensagens foram enviadas
- Quantas mensagens foram processadas
- Quantas falharam e por quê

**Sistema em Geral:**
- Estado dos Circuit Breakers (quais estão abertos)
- Saúde de todos os componentes (banco de dados, fila de mensagens, etc.)

---

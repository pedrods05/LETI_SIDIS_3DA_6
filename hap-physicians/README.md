# hap-physicians — Microserviço de Gestão de Médicos e Consultas

O `hap-physicians` é um **microserviço** que faz parte do sistema **HAP (Hospital Appointment Platform)**, uma plataforma de gestão hospitalar. Este serviço é responsável por:

- **Gerenciar médicos**: Registrar novos médicos, consultar informações, atualizar dados
- **Gerenciar consultas futuras**: Criar, atualizar, cancelar e consultar agendamentos de consultas
- **Validar disponibilidade**: Verificar horários disponíveis para agendamento

## Como Executar
### Perfis e Portas

O serviço pode ser executado em **duas instâncias** diferentes (para alta disponibilidade e distribuição de carga):

- **Instance 1**: Porta 8081, Profile `instance1`
- **Instance 2**: Porta 8087, Profile `instance2`

### Comandos de Execução (Windows, cmd.exe)

```cmd
# Terminal 1 - Instance 1
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1

# Terminal 2 - Instance 2
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance2
```

## Endpoints Principais

O serviço expõe os seguintes endpoints HTTP REST:

**Gestão de Médicos:**
- `POST /physicians/register` - Registrar um novo médico
- `GET /physicians/{id}` - Consultar informações de um médico
- `PUT /physicians/{id}` - Atualizar dados de um médico
- `GET /physicians/{id}/slots` - Ver horários disponíveis para agendamento (ver detalhes abaixo)

**Gestão de Consultas:**
- `POST /appointments` - Criar uma nova consulta
- `GET /appointments` - Listar todas as consultas
- `GET /appointments/{id}` - Consultar uma consulta específica
- `PUT /appointments/{id}` - Atualizar uma consulta
- `PUT /appointments/{id}/cancel` - Cancelar uma consulta
- `GET /appointments/upcoming` - Listar consultas futuras
- `GET /physicians/{physicianId}/slots?startDate=20XX-XX-XX&endDate=20XX-XX-XX` - Retorna os horários disponíveis para agendamento de um médico específico.

> **Nota**: Para ver todos os endpoints disponíveis e testá-los, acesse o Swagger UI:
> - Instance 1: `http://localhost:8081/swagger-ui.html`
> - Instance 2: `http://localhost:8087/swagger-ui.html`

## Arquitetura e Design

### O que é Domain-Driven Design (DDD)?

**Domain-Driven Design (DDD)** é uma abordagem de design de software que foca em modelar o software de acordo com o domínio (área de negócio) que ele representa. No nosso caso, o domínio é a gestão hospitalar.

### Evolução: Monolito → Microserviços

O microserviço `hap-physicians` foi concebido através da decomposição de uma arquitetura monolítica hipotética (um único sistema grande), aplicando princípios de **Domain-Driven Design (DDD)** para dividir em serviços menores e mais gerenciáveis.

#### Bounded Context: "Physician & Appointment Management"

**O que é um Bounded Context?**  
Um Bounded Context é um limite claro dentro do qual um modelo de domínio específico se aplica. É como uma "área de responsabilidade" bem definida.

O `hap-physicians` representa um **Bounded Context** focado na gestão de médicos e agendamento de consultas futuras:

- **Gestão de Médicos**: Registro, consulta e manutenção de informações de médicos
- **Agendamento de Consultas**: Criação, atualização, cancelamento e consulta de consultas futuras
- **Relacionamentos**: Associação entre médicos e consultas, validação de disponibilidade

#### Agregados

**O que é um Agregado?**  
Um Agregado é um conjunto de objetos relacionados que são tratados como uma unidade para propósito de mudanças de dados. O **Aggregate Root** é a entidade principal que controla o acesso ao agregado.

1. **Agregado `Physician`** (Aggregate Root)
   - **Entidade Raiz**: `Physician` (o médico)
   - **Relacionamentos**: `Department` (departamento), `Specialty` (especialidade)
   - **Regras de Negócio**: 
     - Cada médico deve ter uma licença única
     - Cada médico deve estar associado a uma especialidade e um departamento

2. **Agregado `Appointment`** (Aggregate Root)
   - **Entidade Raiz**: `Appointment` (a consulta)
   - **Referências**: 
     - `patientId` (referência externa ao serviço `hap-patients`)
     - `Physician` (referência interna ao médico)
   - **Regras de Negócio**: 
     - Validação de conflitos de horário
     - Verificação de disponibilidade do médico
     - Estados possíveis: `SCHEDULED` (agendada), `CANCELED` (cancelada), `COMPLETED` (concluída)

#### Justificativa do Microserviço

O `hap-physicians` existe como microserviço independente porque:

1. **Responsabilidade Única**: Foco exclusivo em médicos e agendamentos futuros
2. **Ciclo de Vida Independente**: Desenvolvimento, teste e deploy independentes
3. **Escalabilidade Específica**: Escalável conforme demanda de agendamentos
4. **Tecnologias Específicas**: Permite CQRS com MongoDB para read models
5. **Bounded Context Claro**: Domínio bem definido com linguagem ubíqua

**Limites**:
- **Dentro do escopo**: Registro de médicos, gestão de consultas futuras, validações
- **Fora do escopo**: Gestão de pacientes (`hap-patients`), registros médicos (`hap-appointmentrecords`), autenticação (`hap-auth`)

## CQRS - Command Query Responsibility Segregation

### O que é CQRS?

**CQRS (Command Query Responsibility Segregation)** é um padrão arquitetural que **separa operações de leitura (queries) das operações de escrita (commands)**.

**Por que usar CQRS?**
- **Otimização**: Podemos otimizar leitura e escrita de forma independente
- **Escalabilidade**: Podemos escalar leitura e escrita separadamente
- **Performance**: Read models podem ser desnormalizados para consultas mais rápidas
- **Flexibilidade**: Podemos usar diferentes tecnologias para leitura e escrita

**Como funciona no nosso projeto:**
- **Commands (Escrita)**: Modificam dados, salvam no banco de escrita (H2)
- **Queries (Leitura)**: Apenas consultam dados, leem do banco de leitura (MongoDB)

### Commands (Escrita) - Modificar Dados

Commands são operações que **modificam o estado** do sistema (criar, atualizar, deletar). Eles são implementados em `PhysicianCommandService` e `AppointmentCommandService`.

**Endpoints que usam Commands** (ver lista completa na seção "Endpoints Principais"):
- `POST /physicians/register` → Salva no Write Model (H2) e publica `PhysicianRegisteredEvent`
- `PUT /physicians/{id}` → Atualiza e publica `PhysicianUpdatedEvent`
- `POST /appointments` → Salva no Write Model e publica `AppointmentCreatedEvent`
- `PUT /appointments/{id}` → Atualiza e publica `AppointmentUpdatedEvent`
- `PUT /appointments/{id}/cancel` → Cancela e publica `AppointmentCanceledEvent`

**Características importantes:**
- Todos os commands são **transacionais** (ou tudo acontece ou nada acontece)
- Garantem **consistência imediata** no Write Model (H2/JPA)
- Após salvar, publicam eventos para atualizar o Read Model de forma assíncrona

### Queries (Leitura) - Consultar Dados

Queries são operações que **apenas consultam dados** sem modificá-los. Elas são implementadas em `PhysicianQueryService` e `AppointmentQueryService`.

**Endpoints que usam Queries** (ver lista completa na seção "Endpoints Principais"):
- `GET /physicians/{id}` → Consulta Read Model (MongoDB) com fallback para Write Model
- `GET /appointments` → Consulta Read Model
- `GET /appointments/{id}` → Consulta Read Model com fallback
- `GET /appointments/upcoming` → Query específica otimizada
- `GET /physicians/{id}/slots` → Calcula slots disponíveis

**Características importantes:**
- As queries consultam **preferencialmente o Read Model (MongoDB)**, otimizado para leitura rápida
- Se o Read Model não tiver os dados, fazem **fallback** para o Write Model (H2)
- Nunca modificam dados, apenas retornam informações

### Separação de Modelos - Write Model e Read Model

Para implementar CQRS, usamos **dois bancos de dados diferentes**, cada um otimizado para seu propósito:

#### Write Model (H2/JPA) - Banco de Escrita

**Tecnologia**: H2 (banco de dados relacional em memória, para desenvolvimento)  
**Uso**: Operações de escrita (Commands)

**Características:**
- **Garante integridade referencial**: Relacionamentos entre tabelas são validados
- **Consistência transacional**: Operações são atômicas (ACID)
- **Modelos**: `Physician`, `Appointment` (entidades JPA - Java Persistence API)
- **Estrutura**: Normalizada (evita duplicação de dados)
- **Por instância**: Cada instância tem seu próprio banco H2 (isolamento de transações)

**Exemplo**: Quando registramos um médico, os dados são salvos aqui primeiro, garantindo que tudo está correto.

#### Read Model (MongoDB) - Banco de Leitura

**Tecnologia**: MongoDB (banco de dados NoSQL)  
**Uso**: Operações de leitura (Queries)

**Características:**
- **Desnormalizado**: Dados duplicados intencionalmente para performance
- **Otimizado para consultas**: Estrutura pensada para leitura rápida
- **Modelos**: `PhysicianSummary`, `AppointmentSummary` (documentos MongoDB)
- **Atualizado assincronamente**: Atualizado via eventos do RabbitMQ (não imediatamente)
- **Compartilhado**: Ambas as instâncias conectam ao mesmo database `happhysicians_db` (consistência de leitura)

**Exemplo**: Quando consultamos um médico, lemos daqui para ter resposta mais rápida.

**Por que dois bancos?**
- **Write Model**: Garante que os dados estão corretos e consistentes
- **Read Model**: Garante que as consultas são rápidas e eficientes
- **Separação de responsabilidades**: Cada banco faz o que faz melhor

**Estratégia de Bancos de Dados:**
- **H2 Separado**: Isolamento de dados de escrita, evita conflitos, permite processamento paralelo
- **MongoDB Compartilhado**: Consistência de leitura entre instâncias, read model representa estado agregado
- **O que acontece ao remover instância**: 
  - H2: Dados perdidos (in-memory), mas eventos já publicados estão no RabbitMQ
  - MongoDB: Dados preservados (compartilhado)
  - Eventos: Preservados no RabbitMQ, podem ser processados por outras instâncias

## Eventos e RabbitMQ

### O que é RabbitMQ?

**RabbitMQ** é um **message broker** (corretor de mensagens) que permite comunicação assíncrona entre componentes usando o protocolo AMQP (Advanced Message Queuing Protocol).

**Analogia simples**: É como um "correio" onde:
- Componentes enviam "cartas" (eventos)
- O correio (RabbitMQ) entrega as cartas aos destinatários corretos
- Cada destinatário tem uma "caixa de correio" (queue)

### Configuração

- **Exchange**: `hap-exchange` (tipo: Topic Exchange)
  - **O que é Exchange?** É o "centro de distribuição" que roteia mensagens para as queues corretas
- **Configuração**: `RabbitMQConfig.java`
- **Formato de Mensagens**: JSON (usando `Jackson2JsonMessageConverter`)

### Queues (Filas) Criadas Automaticamente

- `q.physician.summary.updater` → Consumida por `PhysicianEventHandler`
- `q.appointment.summary.updater` → Consumida por `AppointmentEventHandler`
- `q.appointment.reminders` → Consumida por `AppointmentReminderHandler`

Uma Queue é uma fila onde os eventos ficam aguardando serem processados. Cada handler tem sua própria queue.

### Eventos Publicados

Após cada escrita bem-sucedida, um evento é publicado via RabbitMQ:

| Evento | Routing Key | Handler | Ação |
|--------|-------------|---------|------|
| `PhysicianRegisteredEvent` | `physician.registered` | `PhysicianEventHandler` | Atualiza `PhysicianSummary` no MongoDB |
| `PhysicianUpdatedEvent` | `physician.updated` | `PhysicianEventHandler` | Atualiza `PhysicianSummary` no MongoDB |
| `AppointmentCreatedEvent` | `appointment.created` | `AppointmentEventHandler` | Atualiza `AppointmentSummary` no MongoDB |
| `AppointmentUpdatedEvent` | `appointment.updated` | `AppointmentEventHandler` | Atualiza `AppointmentSummary` no MongoDB |
| `AppointmentCanceledEvent` | `appointment.canceled` | `AppointmentEventHandler` | Atualiza status para "CANCELED" no MongoDB |
| `AppointmentReminderEvent` | `appointment.reminder` | `AppointmentReminderHandler` | Envia lembretes de consulta (email/SMS) |


### Consistência Eventual

É quando os dados não ficam sincronizados imediatamente, mas eventualmente (em alguns milissegundos) ficam consistentes.

**No nosso caso:**
- Write Model é atualizado **imediatamente** (consistência forte).
- Read Model é atualizado **assincronamente** (consistência eventual).
- **Fallback**: Se o Read Model ainda não tiver os dados, consultamos o Write Model.

É aceitavél uma vez que:
- Consultas são muito mais rápidas no Read Model.
- A diferença de tempo é mínima (milissegundos).
- O fallback garante que sempre temos dados corretos.

## Múltiplas Instâncias

### Configuração
- **Instance 1**: Porta 8081, Profile `instance1`
- **Instance 2**: Porta 8087, Profile `instance2`

### Compartilhamento de Recursos
- **RabbitMQ**: Mesmo exchange e queues (event-driven)
- **MongoDB**: Mesmo database para Read Model
- **H2**: Banco separado por instância (isolamento de escrita)

### Peer-forwarding

Se um recurso não existir na instância local, o serviço tenta buscar nos peers (outras instâncias) usando endpoints públicos. Isso garante alta disponibilidade mesmo se uma instância não tiver os dados.

## Comunicação Externa (HTTP REST)

**O que é comunicação externa?**  
Além de gerenciar médicos e consultas, o serviço precisa se comunicar com outros microserviços para:
- Obter informações de pacientes.
- Validar registros médicos.
- Criar usuários de autenticação.

**ExternalServiceClient** é a classe que gerencia todas essas comunicações HTTP:

| Microserviço | Endpoint | Propósito |
|--------------|----------|-----------|
| **hap-patients** | `GET /patients/{id}` ou `/internal/patients/{id}` | Enriquecer dados de pacientes nas consultas |
| **hap-appointmentrecords** | `GET /api/appointment-records/{id}` | Validar registros médicos |
| **hap-auth** | `POST /auth/users` | Criar usuários de autenticação ao registrar médicos |

**Propagação de Headers**:  
Headers de segurança (`Authorization`, `X-User-Id`, `X-User-Role`) são propagados automaticamente para manter o contexto de autenticação entre serviços.

**Estratégia de Fallback**:  
1. Tenta primeiro endpoint interno `/internal/patients/{id}` (mais rápido, mesma rede).
2. Se falhar, tenta endpoint público `/patients/{id}` (mais lento, mas funciona).

## Análise: Síncrono vs Assíncrono
- A comunicação síncrona é quando o remetente **espera** a resposta antes de continuar. Como uma ligação telefônica - você fala e espera a resposta.
- A comunicação assíncrona é quando o remetente **não espera** a resposta. Como enviar um email - você envia e continua fazendo outras coisas.

### Síncrono (HTTP REST) - Usado para:

- ✅ **Validações críticas em tempo real**: Ex: verificar se paciente existe antes de criar consulta
- ✅ **Operações transacionais**: Ex: criar usuário de autenticação (precisa confirmar que foi criado)
- ✅ **Enriquecimento de dados**: Ex: buscar dados do paciente para retornar na resposta imediata

**Características**: Resposta imediata, garante que a operação foi concluída antes de continuar.

### Assíncrono (RabbitMQ) - Usado para:

- ✅ **Atualização de Read Models**: Ex: atualizar MongoDB após escrever no H2
- ✅ **Desacoplamento de componentes**: Write Side não precisa conhecer Read Side diretamente
- ✅ **Processamento em background**: Ex: enviar lembretes de consulta por email

**Características**: Não bloqueia, permite processamento paralelo, eventualmente consistente.

### Matriz de Decisão 

| Critério | Síncrono (REST) | Assíncrono (RabbitMQ) |
|----------|-----------------|----------------------|
| **Precisa de resposta imediata?** | ✅ Sim | ❌ Não |
| **Precisa de consistência transacional?** | ✅ Sim | ❌ Não (aceita consistência eventual) |
| **É validação crítica?** | ✅ Sim | ❌ Não |
| **É atualização de Read Model?** | ❌ Não | ✅ Sim |
| **Precisa desacoplar componentes?** | ❌ Não | ✅ Sim |

## Configuração e Execução

### Pré-requisitos

1. **RabbitMQ**: `localhost:5672` (guest/guest), Management UI: `http://localhost:15672`
2. **MongoDB**: `localhost:27017`, Database: `happhysicians_db`, User: `root`, Password: `secretpassword`

### Iniciar Serviços

```bash
# Docker Compose 
docker compose up -d

# Ou manualmente
docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:management
docker run -d -p 27017:27017 -e MONGO_INITDB_ROOT_USERNAME=root -e MONGO_INITDB_ROOT_PASSWORD=secretpassword mongo
```

### Executar Instâncias

```cmd
# Terminal 1 - Instance 1
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance1

# Terminal 2 - Instance 2
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=instance2
```
### Verificar Funcionamento

1. **Logs**: Procurar por `✅ Exchange 'hap-exchange' declarado`, `📥 [Query Side] Recebi evento`
2. **RabbitMQ UI**: `http://localhost:15672` - Verificar exchange e queues
3. **MongoDB**: Verificar collections `physician_summaries`, `appointment_summaries`
4. **Swagger**: `http://localhost:8081/swagger-ui.html` - Testar endpoints

## Testes de Integração

Os testes de integração são testes que verificam se os diferentes componentes do sistema funcionam corretamente juntos (banco de dados, RabbitMQ, outros serviços, etc.).

Testes disponíveis no **Hoppscotch** (ferramenta para testar APIs) cobrindo:

1. **CQRS - Commands**: Verificar se a escrita no Write Model funciona e se os eventos são publicados.
2. **CQRS - Queries**: Verificar se a leitura do Read Model (MongoDB) funciona corretamente.
3. **Eventos**: Verificar se a sincronização entre Write Model e Read Model funciona após commands.
4. **Múltiplas Instâncias**: Testar peer-forwarding (buscar dados em outras instâncias) e consistência.
5. **Comunicação Externa**: Verificar se as chamadas HTTP para outros serviços funcionam e se os headers são propagados.

**Como usar**: 
1. Importar coleção de testes SIDIS no Hoppscotch
2. Configurar variáveis de ambiente (URLs dos serviços, etc.)
3. Executar testes na ordem sugerida

## Estrutura de Código
```
hap-physicians/
├── command/          # Command Services (escrita)
├── query/            # Query Services e Read Models (MongoDB)
├── events/           # Eventos e Event Handlers
├── config/           # Configurações (RabbitMQ, MongoDB)
├── repository/       # Repositories do Write Model (JPA)
└── model/            # Entidades do Write Model (JPA)
```

## Limitações conhecidas

As limitações conhecidas são funcionalidades que não foram implementadas (por questões de tempo, escopo, ou complexidade) mas que seriam desejáveis em um ambiente de produção.

| Limitação | Impacto | Solução Futura |
|-----------|---------|----------------|
| **Sem service discovery** | Peers precisam ser configurados manualmente | Implementar Eureka, Consul, ou Kubernetes Service Discovery |
| **Sem circuit breaker/retries** | Se um serviço externo falhar, a requisição falha imediatamente | Implementar Resilience4j ou Hystrix |
| **Sem cache distribuída** | Cada instância faz suas próprias consultas | Implementar Redis ou Hazelcast |
| **H2 in-memory** | Dados perdidos ao reiniciar (apenas para desenvolvimento) | Migrar para PostgreSQL ou MySQL em produção |

**Nota**: Estas limitações são aceitáveis para o contexto educacional do projeto, mas em produção seriam necessárias melhorias.

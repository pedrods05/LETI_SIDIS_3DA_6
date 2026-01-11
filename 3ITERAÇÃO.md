# HAP Platform - Observability, Resilience & Security

Este documento regista as principais decisões arquiteturais tomadas durante a implementação de observabilidade, resiliência e segurança na plataforma HAP, garantindo que o sistema seja monitorizável, resiliente a falhas e seguro para dados sensíveis de saúde.

---

## 1. Observability Integration: Logging, Métricas e Tracing

Numa arquitetura distribuída de microserviços, é fundamental ter visibilidade completa sobre o comportamento do sistema. Implementámos uma estratégia de observabilidade baseada em três pilares: **logging estruturado**, **métricas** e **distributed tracing**.

### 1.1. Logging Estruturado (ELK Stack / Fluentd Ready)

**Decisão:** Adotar logging estruturado com Correlation IDs e Trace IDs, formatado para integração com ferramentas de agregação de logs como ELK Stack (Elasticsearch, Logstash, Kibana) ou Fluentd.

**Justificação:**
- **Correlation IDs:** Cada requisição HTTP recebe um identificador único (`X-Correlation-Id`) que é propagado através de todos os serviços e filas de mensagens. Isto permite filtrar todos os logs relacionados com uma única operação do utilizador, independentemente do microserviço onde ocorreram.
- **Trace IDs:** Integração com Micrometer Tracing gera automaticamente `TraceID` e `SpanID` que são injetados nos logs via MDC (Mapped Diagnostic Context). Estes identificadores conectam-se ao Zipkin para visualização gráfica do fluxo completo.
- **Formato Estruturado:** Os logs são formatados de forma consistente, incluindo timestamp, nível, thread, trace ID, correlation ID e mensagem. Isto facilita a indexação e pesquisa em ferramentas de agregação.

**Benefício:** Permite rastrear uma operação completa desde o pedido inicial até à persistência final, facilitando a depuração de problemas complexos que atravessam múltiplos serviços e filas de mensagens.

### 1.2. Métricas Customizadas (Prometheus + Grafana)

**Decisão:** Implementar métricas customizadas utilizando Micrometer, expostas no formato Prometheus e prontas para visualização no Grafana.

**Métricas Implementadas:**
- **Duração de Operações de Saga:** Mede o tempo de execução de cada passo de uma transação distribuída, permitindo identificar gargalos em fluxos complexos.
- **Contadores de Compensações:** Rastreia quantas vezes o sistema teve que executar ações de compensação (rollback) em transações distribuídas, indicando a estabilidade do sistema.
- **Mensagens AMQP Publicadas/Consumidas:** Monitoriza o volume de eventos enviados e processados através do RabbitMQ, permitindo detetar desequilíbrios entre produção e consumo.
- **Taxa de Falhas:** Contadores de erros por tipo (ex: falhas de autenticação, falhas de comunicação entre serviços).
- **Estados de Circuit Breaker:** Expõe o estado atual de cada Circuit Breaker (CLOSED, OPEN, HALF_OPEN), permitindo alertas quando proteções automáticas são ativadas.

**Justificação:**
- **Prometheus:** Formato padrão da indústria para métricas, com suporte nativo em Kubernetes e ferramentas de monitorização.
- **Métricas Customizadas:** Além das métricas padrão do Spring Boot (CPU, memória, threads), adicionámos métricas específicas do domínio (Saga, AMQP, Circuit Breaker) que são críticas para entender o comportamento do sistema distribuído.
- **Integração com Health Checks:** As métricas de Circuit Breaker são expostas também como health indicators, permitindo que orquestradores (Kubernetes) reajam automaticamente quando serviços estão em falha.

**Benefício:** Permite criar dashboards no Grafana para visualizar a saúde do sistema em tempo real e configurar alertas proativos (ex: "alerta se mais de 10% das mensagens AMQP falharem").

### 1.3. Distributed Tracing (Zipkin)

**Decisão:** Implementar distributed tracing utilizando Micrometer Tracing Bridge com Brave e integração com Zipkin para visualização gráfica de traces.

**Justificação:**
- **Propagação Automática:** O Micrometer Tracing propaga automaticamente Trace IDs e Span IDs através de requisições HTTP (via headers) e mensagens AMQP (via headers de mensagem). Isto garante que uma única operação seja rastreada através de todos os serviços e filas.
- **Visualização Gráfica:** O Zipkin permite visualizar o percurso completo de uma transação, incluindo latências de cada componente (ex: tempo gasto no `hap-patients` vs tempo de espera na fila RabbitMQ vs tempo de escrita no MongoDB).
- **Correlação com Logs:** Os Trace IDs nos logs permitem correlacionar logs com traces no Zipkin, facilitando a depuração de problemas complexos.

**Benefício:** Facilita depurar problemas que envolvem múltiplos serviços, permitindo identificar rapidamente onde ocorrem latências ou falhas numa transação distribuída.

### 1.4. Health Checks (Liveness e Readiness Probes)

**Decisão:** Implementar health checks separados para Liveness e Readiness, expostos via Spring Boot Actuator.

**Justificação:**
- **Liveness Probe:** Verifica se a aplicação está viva (processo em execução). Se falhar, o orquestrador (Kubernetes) reinicia automaticamente o contentor.
- **Readiness Probe:** Verifica se a aplicação está pronta para receber tráfego (base de dados conectada, RabbitMQ acessível, dependências críticas disponíveis). Se falhar, o orquestrador remove a instância do balanceador de carga até que esteja pronta.
- **Health Indicators Customizados:** Integração com Resilience4j permite que Circuit Breakers sejam expostos como health indicators, permitindo que o sistema reaja automaticamente quando proteções de resiliência são ativadas.

**Benefício:** Garante que apenas instâncias saudáveis recebam tráfego e que instâncias com problemas sejam reiniciadas automaticamente, melhorando a disponibilidade do sistema.

---

## 2. Resilience and Fault Tolerance: Circuit Breaker, Retry, Timeout e Bulkhead

Para evitar falhas em cascata (*Cascading Failures*) e garantir que o sistema degrade graciosamente sob carga ou problemas de rede, implementámos padrões de resiliência robustos utilizando a biblioteca **Resilience4j**, integrados explicitamente com o padrão Saga para gestão de transações distribuídas.

### 2.1. Circuit Breaker (Disjuntor)

**Decisão:** Implementar Circuit Breaker para proteger chamadas síncronas a serviços externos e publicações de eventos AMQP.

**Justificação:**
- **Proteção contra Falhas em Cascata:** Quando um serviço está em falha ou lento, o Circuit Breaker monitoriza a taxa de erro. Se exceder um limiar configurado (ex: 50% de falhas), o circuito "abre" e para de fazer chamadas ao serviço problemático, falhando rapidamente ou devolvendo uma resposta de fallback pré-definida.
- **Recuperação Automática:** Após um período de espera configurado, o circuito entra em estado HALF_OPEN e permite algumas tentativas para verificar se o serviço recuperou. Se bem-sucedido, fecha o circuito; caso contrário, reabre.
- **Integração com Health Checks:** O estado do Circuit Breaker é exposto como health indicator, permitindo que orquestradores reajam automaticamente quando serviços estão em falha.

**Aplicação:**
- **Chamadas HTTPS a Serviços Externos:** Protege chamadas ao `hap-auth` e `hap-appointmentrecords`, evitando que falhas nesses serviços causem timeouts prolongados no serviço chamador.
- **Publicações AMQP:** Protege a publicação de eventos no RabbitMQ, evitando que falhas temporárias na fila bloqueiem operações críticas.

**Benefício:** Previne que um serviço com problemas cause uma cascata de falhas em todo o sistema, garantindo que o sistema continue a responder mesmo quando dependências estão indisponíveis.

### 2.2. Retry com Backoff Exponencial

**Decisão:** Implementar Retry automático com backoff exponencial para falhas transientes de rede ou serviços momentaneamente indisponíveis.

**Justificação:**
- **Falhas Transientes:** Muitas falhas em sistemas distribuídos são temporárias (ex: timeout de rede, serviço momentaneamente sobrecarregado). O Retry permite que o sistema tente novamente automaticamente antes de desistir.
- **Backoff Exponencial:** O tempo de espera entre tentativas aumenta exponencialmente (ex: 500ms, 1s, 2s), evitando sobrecarregar serviços que estão a recuperar e reduzindo a contenção de recursos.
- **Limite de Tentativas:** Configurado para tentar até 3 vezes antes de desistir, evitando loops infinitos e garantindo que falhas permanentes sejam reportadas rapidamente.

**Aplicação:**
- **Operações de Saga:** Tentativas automáticas em passos de transações distribuídas que falharam temporariamente.
- **Publicações AMQP:** Retry em publicações de eventos que falharam devido a problemas temporários na fila.

**Benefício:** Lida automaticamente com falhas temporárias de rede ou serviços, melhorando a taxa de sucesso de operações sem intervenção manual.

### 2.3. Timeout (Tempo Limite)

**Decisão:** Implementar TimeLimiter para definir tempos máximos de execução para operações, evitando que fiquem bloqueadas indefinidamente.

**Justificação:**
- **Prevenção de Bloqueios:** Operações que ficam à espera de respostas que nunca chegam consomem recursos (threads, conexões) indefinidamente. O Timeout cancela essas operações após um período configurado.
- **Degradação Graciosa:** Se uma operação exceder o timeout, o sistema pode executar uma ação de fallback ou retornar um erro controlado, em vez de ficar bloqueado.

**Aplicação:**
- **Operações HTTPS:** Timeout de 5-15 segundos para chamadas a serviços externos, evitando que requisições fiquem bloqueadas.
- **Operações de Saga:** Timeout de 30 segundos para transações distribuídas completas, garantindo que operações complexas não fiquem indefinidamente pendentes.

**Benefício:** Previne que recursos fiquem presos esperando respostas que nunca virão, garantindo que o sistema continue responsivo mesmo quando dependências estão lentas ou indisponíveis.

### 2.4. Bulkhead (Compartimento Estanque)

**Decisão:** Implementar Bulkhead para isolar recursos, garantindo que problemas numa área não afetem outras.

**Justificação:**
- **Isolamento de Recursos:** O Bulkhead cria pools de threads separados para diferentes tipos de operações. Se uma área do sistema tiver problemas (ex: muitas compensações de Saga), isso não consome todos os recursos disponíveis, deixando outras operações normais continuarem a funcionar.
- **Proteção contra Esgotamento de Recursos:** Sem Bulkhead, uma área problemática pode esgotar todos os threads disponíveis, causando que o sistema inteiro pare de responder.

**Aplicação:**
- **Operações de Compensação:** Compensações (rollback) de transações Saga rodam num pool de threads separado e limitado. Se houver muitos problemas em compensações, isso não afeta operações normais de criação ou atualização.

**Benefício:** Garante que uma parte problemática do sistema não consuma todos os recursos disponíveis, permitindo que o sistema continue a funcionar parcialmente mesmo quando algumas áreas estão com problemas.

### 2.5. Integração Explícita com Saga Pattern

**Decisão:** Integrar explicitamente os padrões de resiliência com o padrão Saga para gestão de transações distribuídas.

**Justificação:**
- **Resiliência em Publicações de Eventos:** Todas as publicações de eventos AMQP que fazem parte de uma Saga têm Circuit Breaker e Retry, garantindo que eventos críticos sejam publicados mesmo em caso de falhas temporárias na fila.
- **Timeout em Operações de Saga:** Operações de Saga completas têm timeout configurado, evitando que transações distribuídas fiquem indefinidamente pendentes.
- **Isolamento de Compensações:** Compensações (desfazer operações) são isoladas com Bulkhead, garantindo que problemas em rollbacks não afetem operações normais.
- **Métricas de Saga:** Métricas customizadas rastreiam o desempenho e falhas de todas as operações de Saga, permitindo monitorizar a saúde de transações distribuídas.

**Benefício:** Garante que transações distribuídas sejam confiáveis e possam ser monitorizadas, permitindo que o sistema lide graciosamente com falhas em operações complexas que envolvem múltiplos serviços.

---

## 3. Security Implementation: OAuth2, JWT, mTLS e GDPR Compliance

Para garantir que a plataforma HAP seja segura e cumpra com padrões de proteção de dados (especialmente GDPR para dados de saúde), implementámos uma estratégia de segurança em múltiplas camadas: **OAuth2 para autorização**, **JWT para autenticação baseada em tokens**, **mTLS para encriptação mútua em chamadas internas** e **proteção de endpoints sensíveis** com controlo de acesso baseado em roles.

### 3.1. OAuth2 para Autorização

**Decisão:** Adotar OAuth2 Resource Server pattern com Spring Security para autorização de recursos protegidos.

**Justificação:**
- **Padrão da Indústria:** OAuth2 é o padrão de facto para autorização em APIs REST, amplamente suportado por clientes (browsers, aplicações mobile, serviços backend).
- **Separação de Responsabilidades:** O serviço `hap-auth` atua como Authorization Server, emitindo tokens JWT. Os outros serviços (Patients, Physicians, AppointmentRecords) atuam como Resource Servers, validando tokens e autorizando acesso a recursos.
- **Escalabilidade:** Tokens JWT são stateless, permitindo que qualquer instância de um serviço valide um token sem necessidade de comunicação com o Authorization Server para cada requisição.

**Funcionamento:**
- O cliente autentica-se no `hap-auth` e recebe um JWT token.
- O cliente inclui o token no header `Authorization: Bearer <token>` em requisições subsequentes.
- Cada Resource Server valida o token (assinatura, expiração) e extrai claims (roles, user ID) para autorização.

**Benefício:** Fornece autorização robusta e escalável, permitindo que múltiplos serviços validem tokens independentemente sem comunicação centralizada.

### 3.2. JWT para Autenticação Baseada em Tokens

**Decisão:** Utilizar JWT (JSON Web Tokens) para autenticação stateless entre serviços.

**Justificação:**
- **Stateless:** Tokens JWT contêm toda a informação necessária (claims) para autorização, eliminando a necessidade de consultar uma base de dados central para cada requisição.
- **Performance:** Validação de tokens é rápida (verificação de assinatura criptográfica), permitindo alta throughput em sistemas distribuídos.
- **Propagação Automática:** Tokens JWT são propagados automaticamente entre serviços através de interceptors no `RestTemplate`, garantindo que chamadas service-to-service mantenham o contexto de autenticação do utilizador original.

**Funcionamento:**
- Tokens são assinados com uma chave secreta partilhada entre Authorization Server e Resource Servers.
- Claims incluem informações do utilizador (user ID, email) e roles (ADMIN, PHYSICIAN, PATIENT).
- Resource Servers validam a assinatura e extraem claims para controlo de acesso baseado em roles.

**Benefício:** Fornece autenticação eficiente e escalável, permitindo que serviços validem identidade e permissões sem comunicação adicional com o Authorization Server.

### 3.3. mTLS (Mutual TLS) para Encriptação Mútua em Chamadas Internas

**Decisão:** Implementar mTLS para todas as comunicações service-to-service, garantindo encriptação mútua e autenticação baseada em certificados.

**Justificação:**
- **Encriptação de Dados Sensíveis:** Dados de saúde são altamente sensíveis. mTLS garante que todas as comunicações entre serviços sejam encriptadas, mesmo dentro da rede interna, protegendo contra eavesdropping e man-in-the-middle attacks.
- **Autenticação Mútua:** mTLS requer que tanto o cliente quanto o servidor apresentem certificados válidos. Isto garante que apenas serviços autorizados (com certificados válidos no truststore) possam comunicar entre si, prevenindo acesso não autorizado mesmo se um atacante conseguir acesso à rede interna.
- **Proteção de Endpoints Internos:** Endpoints `/internal/**` são protegidos por mTLS (configuração `server.ssl.client-auth=need`), garantindo que apenas serviços com certificados válidos possam acessá-los. Endpoints públicos (Swagger, H2 Console) usam `server.ssl.client-auth=want` para permitir acesso via browser sem certificado.

**Funcionamento:**
- Cada serviço possui um keystore (certificado próprio) e um truststore (certificados de serviços confiáveis).
- Quando um serviço faz uma chamada HTTPS a outro, o cliente apresenta o seu certificado do keystore e valida o certificado do servidor contra o truststore.
- Apenas serviços com certificados válidos no truststore do servidor podem estabelecer conexão.

**Benefício:** Fornece encriptação e autenticação robustas para comunicações service-to-service, garantindo que dados sensíveis sejam protegidos mesmo dentro da rede interna.

### 3.4. Role-Based Access Control (RBAC) para Endpoints Sensíveis

**Decisão:** Implementar controlo de acesso baseado em roles utilizando `@PreAuthorize` do Spring Security para proteger endpoints sensíveis.

**Justificação:**
- **Princípio do Menor Privilégio:** Cada endpoint requer apenas as roles necessárias para a operação. Por exemplo, apenas ADMIN pode eliminar consultas, enquanto PHYSICIAN e PATIENT podem criar consultas.
- **Proteção de Dados Sensíveis:** Endpoints que acedem a dados sensíveis (ex: perfis completos de pacientes, notas de consultas) são protegidos com roles específicas, garantindo que apenas utilizadores autorizados possam acessá-los.
- **GDPR Compliance:** Controlo de acesso granular permite garantir que apenas pessoal autorizado (médicos, administradores) acede a dados de saúde, cumprindo requisitos de GDPR.

**Aplicação:**
- **Endpoints de Consultas:** `POST /appointments` requer ADMIN, PHYSICIAN ou PATIENT; `GET /appointments` requer ADMIN ou PHYSICIAN; `PUT /appointments/{id}/cancel` requer apenas ADMIN.
- **Endpoints de Pacientes:** `GET /patients/{id}/profile` requer PHYSICIAN (médicos podem ver perfis completos); `PATCH /patients/me` requer PATIENT (pacientes podem atualizar apenas o seu próprio perfil).
- **Endpoints de Auditoria:** `GET /appointments/{id}/audit-trail` requer ADMIN ou PHYSICIAN, garantindo que apenas pessoal autorizado pode ver histórico de alterações.

**Benefício:** Garante que dados sensíveis sejam acessíveis apenas a utilizadores autorizados, cumprindo requisitos de segurança e GDPR.

### 3.5. GDPR Compliance para Dados de Saúde

**Decisão:** Implementar medidas de conformidade com GDPR para proteção de dados de saúde, incluindo audit trails, proteção de endpoints sensíveis e controlo de consentimento.

**Justificação:**
- **Audit Trails:** Todas as operações sobre dados sensíveis (criação, atualização, cancelamento de consultas; registo e atualização de pacientes) são registadas num Event Store imutável, permitindo rastrear quem acedeu ou modificou dados e quando. Isto cumpre requisitos de GDPR para accountability e direito de acesso a dados pessoais.
- **Proteção de Endpoints Sensíveis:** Endpoints que acedem a dados de saúde são protegidos com mTLS (comunicações encriptadas) e RBAC (apenas roles autorizadas), garantindo que apenas pessoal autorizado acede a dados sensíveis.
- **Event Sourcing para Auditoria:** O Event Store funciona como trilho de auditoria completo, permitindo reconstruir o histórico completo de alterações a dados pessoais, cumprindo requisitos de GDPR para direito de acesso e portabilidade de dados.

**Funcionamento:**
- **Event Store:** Cada operação crítica (ex: `AppointmentCreated`, `NoteAdded`, `PatientRegistered`) é persistida no Event Store com metadados de auditoria (timestamp, user ID, correlation ID).
- **Audit Trail Endpoints:** Endpoints como `GET /appointments/{id}/audit-trail` permitem visualizar o histórico completo de alterações, protegidos por RBAC (apenas ADMIN ou PHYSICIAN).
- **Encriptação em Trânsito:** mTLS garante que dados sensíveis sejam encriptados durante transmissão entre serviços.

**Benefício:** Garante conformidade com GDPR para dados de saúde, permitindo rastreabilidade completa de acessos e alterações a dados pessoais e garantindo que apenas pessoal autorizado acede a dados sensíveis.

---
---

---

## 4. Governance, API Evolution e SLAs

Para garantir a sustentabilidade, a qualidade do serviço e a manutenibilidade a longo prazo da plataforma HAP, implementámos um framework de governação focado em métricas de desempenho e na gestão segura da evolução das interfaces.

### 4.1. Service Level Agreements (SLAs)

**Decisão:** Definir e monitorizar indicadores de nível de serviço (SLIs) e objetivos (SLOs) claros para as operações críticas, utilizando a infraestrutura de métricas já implementada.

**Métricas de Compromisso (Targets):**
- **Disponibilidade (Uptime):** Objetivo de **99.5%** de uptime mensal para todos os serviços, monitorizado através das *Readiness Probes* do Actuator.
- **Latência de Agendamento (Saga):** Transações distribuídas de criação de consulta (envolvendo múltiplos serviços) devem completar-se em menos de **2 segundos** no percentil 95 (p95).
- **Taxa de Erro:** Máximo de **1%** de pedidos resultantes em HTTP 5xx em condições normais de carga.
- **Resiliência:** O tempo médio de recuperação (MTTR) em caso de falha de uma instância deve ser inferior a **5 minutos**, garantido pela auto-recuperação e pelos *Circuit Breakers*.

**Justificação:** No domínio da saúde, a rapidez no acesso a registos clínicos e a fiabilidade no agendamento são essenciais para evitar ineficiências operacionais e riscos de conformidade.

### 4.2. Plano de Depreciação e Evolução de API

**Decisão:** Adotar uma estratégia de versionamento explícito e um ciclo de vida de depreciação para evitar a interrupção de consumidores externos e internos.

**Estratégia Implementada:**
- **Versionamento por URL:** Uso de prefixos na rota (ex: `/api/v2/patients/register`) para permitir que novas funcionalidades coexistam com versões legadas sem quebras de contrato.
- **Ciclo de Depreciação:** 1. **Anúncio:** Uma versão é marcada como depreciada na documentação OpenAPI/Swagger.
    2. **Período de Grace:** A versão antiga é mantida ativa por um período definido (ex: 6 meses).
    3. **Monitorização:** Através de métricas customizadas, identificamos se ainda existe tráfego na versão antiga antes da sua desativação definitiva.

### 4.3. Governação de Contratos e Descoberta

**Decisão:** Utilizar especificações OpenAPI para documentação e Testes de Contrato para garantir a integridade das interações entre serviços.

**Justificação:**
- **API Contracts:** Cada microserviço expõe a sua especificação Swagger/OpenAPI, servindo como contrato formal entre equipas e serviços.
- **Contract Testing (Pact):** Implementação de testes de contrato (Consumer-Driven Contracts) utilizando Pact para validar que alterações no fornecedor (Provider) não quebram os requisitos do consumidor (Consumer) antes do deployment.
- **Service Catalog:** Documentação centralizada para facilitar a descoberta de serviços e a gestão de responsabilidades (ownership) dentro da arquitetura distribuída.

### 4.4. Governação de APIs e Testes de Contrato (Pact)

**Decisão:** Implementar uma estratégia de "Consumer-Driven Contracts" utilizando a framework Pact para garantir a compatibilidade entre microserviços.

**Justificação:**
- **Prevenção de Quebras:** Em sistemas distribuídos, alterações num serviço (Provider) podem quebrar outros (Consumers). Os testes de contrato garantem que o fornecedor cumpre exatamente o que o consumidor espera antes de qualquer alteração ser validada.
- **Documentação Executável:** Os contratos servem como documentação técnica viva e sempre atualizada das interações reais entre os serviços.

**Implementação:**
- **Pact Consumer:** Implementado no lado dos serviços que dependem de dados externos (ex: `hap-appointmentrecords` a consumir de outros), definindo as expectativas de resposta (ver `PactConsumerTest.java`).
- **Pact Provider:** Implementado no lado dos serviços que fornecem dados (ex: `hap-patients`), validando se a API atual ainda satisfaz os contratos gerados pelos consumidores (ver `PactProviderTest.java`).
- **OpenAPI/Swagger:** Utilizado complementarmente para a descoberta manual e testes exploratórios de cada serviço através da interface visual.

### 4.5. Service Catalog e Descoberta

**Decisão:** Centralizar a definição de metadados dos serviços através de um catálogo inspirado no Backstage.

**Funcionamento:**
- Cada serviço inclui um ficheiro `catalog-info.yaml` que define o seu ownership, dependências e tipo de API.
- Isto facilita a governação a longo prazo, permitindo que novos membros da equipa percebam rapidamente a árvore de dependências do sistema HAP sem necessidade de analisar o código fonte.
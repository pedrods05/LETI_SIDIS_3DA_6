# ADR 0004 — Adopção de CQRS + AMQP (RabbitMQ) para hap-appointmentrecords

Status: Aceite
Data: 2025-12-04

Contexto
--------
O `hap-appointmentrecords` é responsável pelo armazenamento e disponibilização de registos de consultas. Para melhorar escalabilidade, desempenho das consultas e integração entre domínios, foi necessária uma solução para sincronizar o write-model (transacional) com um read-model otimizado e para propagar eventos de negócio a outros serviços.

Decisão
-------
Adotar CQRS (Command Query Responsibility Segregation) no módulo `hap-appointmentrecords` com eventos assíncronos transportados via RabbitMQ (AMQP). Implementação inicial sem Event Sourcing (persistimos no write DB e publicamos eventos que constroem projeções para leitura).

Justificativa
------------
- Separação clara entre regras de negócio (write) e otimização de consultas (read).
- Possibilidade de escalar independentemente consumidores/servidores de leitura.
- Desacoplamento temporal entre produtores e consumidores (outros serviços podem escutar os mesmos eventos sem ligação direta).
- Permite evoluir para event sourcing no futuro se necessário.

Implicações
-----------
- A aplicação passa a publicar eventos como `AppointmentCreatedEvent` após gravação no write-model.
- Um listener consumidor atualiza a projeção (`AppointmentProjection`) que servirá as queries rápidas.
- As queries podem usar uma base otimizada (projeções desnormalizadas) sem impactar transações de escrita.
- A consistência entre write/read passa a ser eventual; UIs devem tratar possíveis latências.

Detalhes de implementação (resumo técnico)
-----------------------------------------
- Dependências: `spring-boot-starter-amqp` (RabbitMQ), Jackson message converter.
- Beans AMQP: `TopicExchange`, `Queue`, `Binding`, `RabbitTemplate` com `Jackson2JsonMessageConverter`.
- Entidades/Classes principais:
  - `CreateAppointmentCommand` + `CreateAppointmentCommandHandler` (write side)
  - `AppointmentCreatedEvent` (evento publicado)
  - `AppointmentEventsListener` (consumer que actualiza `AppointmentProjection`)
  - `AppointmentProjection` + `AppointmentProjectionRepository` (read model)
- Testes: Unit tests para handler e listener; integração com Testcontainers RabbitMQ recomendada para CI.

Riscos e mitigação
------------------
- Eventual consistency: Mitigar na UI com estados e leituras opcionais do write-model quando necessário.
- Duplicidade de mensagens: Implementar idempotência na projeção (upsert por appointmentId, ou eventId dedup).
- Operacional: gerir RabbitMQ (DLQ, retry, TLS, credenciais) e monitorizar filas.

Estado atual
------------
Implementação inicial entregue: publicador no handler, listener que grava projeção local, configuração AMQP e testes unitários. Próximos passos incluem ativar listeners via `@RabbitListener` em runtime, criar testes de integração com Testcontainers e adicionar políticas de retry/DLQ.

Referências
----------
- Arquitetura: `2ITERAÇÃO.md` (raiz do repositório)
- Código: `hap-appointmentrecords/src/main/java/leti_sisdis_6/hapappointmentrecords/`

Decisão tomada por: equipa HAP (implementador)

---


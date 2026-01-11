# ADR 0004 — Adoção Global de CQRS e Event-Driven Architecture (RabbitMQ)

Status: Aceite
Data: 2025-12-04

Contexto
--------
A plataforma HAP requer alta disponibilidade para leituras (consultas de pacientes, médicos e agendamentos) e integridade estrita para escritas (registos, atualizações). A arquitetura monolítica ou puramente síncrona cria acoplamento temporal e dificulta a escalabilidade de leitura independente da escrita.

Decisão
-------
Adotar o padrão **CQRS (Command Query Responsibility Segregation)** transversalmente aos microserviços principais (`hap-patients`, `hap-physicians`, `hap-appointmentrecords`), suportado por uma arquitetura orientada a eventos (EDA) utilizando **RabbitMQ**.

Justificação
------------
- **Desempenho Assimétrico:** O volume de leituras é muito superior ao de escritas. O CQRS permite otimizar o *Read Model* (MongoDB desnormalizado) separadamente do *Write Model* (Relacional normalizado).
- **Desacoplamento:** O uso de eventos (AMQP) permite que os serviços de leitura sejam atualizados sem bloquear a transação de escrita.
- **Evolutividade:** Facilita a introdução futura de novos consumidores (ex: serviço de Notificações ou Analytics) sem alterar os emissores.

Implicações
-----------
1. **Infraestrutura:** Necessidade de manter RabbitMQ e MongoDB partilhado (ou por instância).
2. **Complexidade:** A aplicação deve gerir dois modelos de dados (JPA e Mongo Document) e garantir a sua sincronização via `EventHandlers`.
3. **Consistência Eventual:** A UI deve estar preparada para ligeiros atrasos entre a escrita e a disponibilidade do dado na leitura.

Detalhes de Implementação
-------------------------
- **Write Side:** H2/PostgreSQL (JPA) garante ACID. Publica eventos (`PatientRegistered`, `AppointmentCreated`) no Exchange `hap-exchange`.
- **Read Side:** Listeners (`PatientEventHandler`, etc.) consomem eventos e atualizam projeções (`*Summary`) no MongoDB.

Contexto
--------
A plataforma HAP adotou o padrão arquitetural **CQRS** (Command Query Responsibility Segregation).
O modelo de escrita (Command) utiliza bases de dados relacionais (JPA/SQL) para garantir integridade referencial e transações ACID. No entanto, o modelo relacional normalizado não é eficiente para operações de leitura intensiva, exigindo *JOINs* complexos e múltiplos acessos à base de dados para construir vistas simples para o utilizador (ex: um perfil de médico que inclui especialidade, departamento e lista de agendamentos).
Além disso, existe um requisito não-funcional do projeto para a utilização de **MongoDB**.

Decisão
-------
Adotar o **MongoDB** como a tecnologia de persistência padrão para o **Read Model (Query Side)** em todos os microserviços principais da plataforma (`hap-patients`, `hap-physicians`, `hap-appointmentrecords`).

1.  **Projeções Desnormalizadas:** Os dados são armazenados em documentos JSON (Projeções ou *Summaries*) que já contêm toda a informação necessária para uma determinada vista da UI, eliminando a necessidade de *JOINs* em tempo de leitura.
2.  **Sincronização por Eventos:** As coleções do MongoDB são atualizadas de forma assíncrona através de *Event Handlers* que reagem a eventos de domínio (ex: `PatientRegistered`, `AppointmentCreated`) emitidos pelo modelo de escrita.

Justificativa
------------
- **Performance de Leitura:** O MongoDB permite leituras extremamente rápidas (O(1)) de documentos complexos por ID ou índices simples, ideal para APIs de *Experience* que servem o Frontend.
- **Flexibilidade de Esquema:** Facilita a evolução das vistas de leitura (ex: adicionar um campo novo ao resumo do paciente) sem necessidade de migrações de esquema rígidas como em SQL.
- **Alinhamento com CQRS:** A natureza documental do MongoDB mapeia diretamente para os DTOs de leitura, simplificando a camada de *Query Service*.

Consequências
-------------
- **Duplicação de Dados:** Aceitamos a redundância de dados (o mesmo dado existe no SQL e no Mongo) em troca de performance de leitura.
- **Gestão de Infraestrutura:** É necessário gerir contentores MongoDB adicionais (partilhados ou por instância) no ambiente de desenvolvimento e produção.
- **Consistência Eventual:** As leituras feitas no MongoDB podem estar ligeiramente desatualizadas em relação ao SQL (milissegundos de atraso) enquanto os eventos são processados pelo RabbitMQ.

Exemplos de Implementação
-------------------------
- **hap-patients:** Coleção `patient_summaries` armazena o perfil completo do paciente.
- **hap-physicians:** Coleção `appointment_summaries` armazena detalhes da consulta desnormalizados (incluindo nomes de médico/paciente) para listagens rápidas.
- **hap-appointmentrecords:** Coleção `appointment_record_projections` armazena o histórico clínico para acesso rápido.

Contexto
--------
A plataforma HAP é um sistema complexo com múltiplas responsabilidades distintas: autenticação, gestão de pacientes, gestão de médicos e o registo clínico de consultas. Uma arquitetura monolítica ou com fronteiras mal definidas levaria a um acoplamento forte, dificultando a manutenção e a evolução independente de cada área de negócio.

Decisão
-------
Adotar **Domain-Driven Design (DDD)** para decompor o sistema em quatro **Bounded Contexts** autónomos, implementados como microserviços independentes:

1.  **Auth Context (`hap-auth`):**
    * **Responsabilidade:** Gestão de identidades, credenciais, roles e emissão de tokens de segurança.
    * **Dados:** Users, Roles.
2.  **Patients Context (`hap-patients`):**
    * **Responsabilidade:** Gestão do ciclo de vida e perfil dos pacientes (incluindo dados sensíveis/PII).
    * **Dados:** Patient Profile, Medical History (resumo), Contacts.
3.  **Physicians Context (`hap-physicians`):**
    * **Responsabilidade:** Gestão de perfis médicos, especialidades e agendamento de consultas futuras (disponibilidade).
    * **Dados:** Physician Profile, Availability, Scheduled Appointments.
4.  **AppointmentRecords Context (`hap-appointmentrecords`):**
    * **Responsabilidade:** Arquivo histórico e clínico de consultas realizadas.
    * **Dados:** Clinical Records, Diagnoses, Prescriptions.

Consequências e Padrões de Interação
------------------------------------
- **Share-Nothing Architecture:** Cada Bounded Context possui a sua própria base de dados e não partilha esquema nem tabelas com outros serviços.
- **Referência por ID:** Os serviços não armazenam dados duplicados de outros domínios (ex: `hap-appointmentrecords` guarda apenas `patientId` e `physicianId`, não o nome ou morada).
- **Comunicação Híbrida:**
    - **Síncrona (REST):** Para validações em tempo real e obtenção de dados de referência (ex: obter nome do médico a partir do ID para exibir na UI).
    - **Assíncrona (Eventos):** Para consistência eventual e efeitos colaterais (ex: quando um paciente é registado, emitir evento para criar modelos de leitura).

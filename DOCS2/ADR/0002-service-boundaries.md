Contexto
--------
Na fase inicial do projeto, alguns módulos partilhavam bibliotecas de domínio ou tinham dependências diretas de compilação (ex: `hap-appointmentrecords` a depender de `hap-patients`). Isto viola os princípios de microserviços, criando um "monolito distribuído" onde uma alteração num modelo obriga a recompilar e redeployar múltiplos serviços, impedindo a evolução independente.

Decisão
-------
Adotar uma arquitetura **Share-Nothing** estrita para todos os microserviços (`hap-auth`, `hap-patients`, `hap-physicians`, `hap-appointmentrecords`).

1.  **Zero Dependências de Compilação:** Nenhum microserviço pode declarar outro como dependência no `pom.xml`.
2.  **Duplicação de DTOs:** Cada serviço deve definir os seus próprios DTOs (Data Transfer Objects) para representar dados externos, mesmo que isso implique duplicação de código.
3.  **Comunicação Exclusiva por Contratos:** A interação é feita exclusivamente via APIs públicas (REST) ou Eventos (AMQP), nunca por partilha de classes Java.

Justificativa
------------
- **Autonomia de Deploy:** Permite que uma equipa atualize o serviço de Pacientes sem ter de coordenar o deploy com a equipa de Médicos.
- **Tolerância a Falhas:** Se um serviço alterar a sua estrutura interna de dados, não quebra os consumidores desde que a API pública (o contrato) se mantenha.
- **Escalabilidade:** Permite escalar componentes individualmente sem arrastar dependências pesadas.

Consequências
-------------
- **Aumento de Código (Boilerplate):** Temos de criar classes como `PatientDTO` tanto no serviço de *Patients* (quem envia) como no de *Physicians* (quem recebe).
- **Validação em Tempo de Execução:** Erros de integração (ex: mudança de nome de campo JSON) só são detetados em tempo de execução ou testes de integração, e não em tempo de compilação.
- **Necessidade de Clientes HTTP:** Todos os serviços utilizam `RestTemplate` (ou WebClient) para obter dados de outros domínios, em vez de chamadas de método diretas.

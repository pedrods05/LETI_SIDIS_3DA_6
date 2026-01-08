# ADR 0006 — Persistência Poliglota (H2 + MongoDB)

Status: Aceite
Data: 2025-12-04

Contexto
--------
Diferentes operações do sistema têm requisitos de dados opostos:
- Comandos (Registo, Agendamento) exigem integridade referencial forte e transações atómicas.
- Consultas (Listagens, Detalhes) exigem flexibilidade de esquema e alta velocidade de leitura.

Decisão
-------
Utilizar uma abordagem de **Persistência Poliglota** dentro de cada microserviço:
1. **Base Relacional (H2 em Dev / PostgreSQL em Prod):** Para o *Write Model*.
2. **Base Orientada a Documentos (MongoDB):** Para o *Read Model* (Projeções).

Justificação
------------
- **Integridade:** O modelo relacional é superior para garantir regras de negócio complexas e relações (ex: Médico tem de ter Especialidade).
- **Performance:** O MongoDB permite guardar o objeto pronto a ser consumido pelo Frontend (JSON), evitando JOINs complexos em tempo de leitura.

Implicações
-----------
- A infraestrutura de desenvolvimento torna-se mais pesada (necessidade de contentor Mongo).
- O código de *bootstrap* tem de garantir que ambas as bases de dados estão num estado consistente no arranque (ou limpar dados antigos).

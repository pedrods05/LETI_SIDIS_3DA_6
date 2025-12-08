# ADR 0005 — Estratégia de Resiliência e Disponibilidade (Peer Forwarding)

Status: Aceite
Data: 2025-12-06

Contexto
--------
Num ambiente distribuído com múltiplas instâncias de cada serviço (peers), uma instância pode não ter os dados mais recentes na sua base de dados local (devido a latência de replicação ou particionamento de rede), ou a base de dados de leitura pode estar indisponível.

Decisão
-------
Implementar um mecanismo de **"Fallback em Cascata" com Peer Forwarding** nos controladores de leitura (`GET`).
A ordem de resolução é:
1. **Read Model (MongoDB):** Rápido e preferencial.
2. **Cache Local/Write Model (SQL):** Fonte de verdade local.
3. **Peer Forwarding (HTTP):** Consulta direta síncrona a outras instâncias conhecidas do mesmo serviço.

Justificativa
------------
- **Teorema CAP:** Em caso de falha parcial, priorizamos a **Disponibilidade (Availability)** sobre a Consistência imediata.
- **Redundância:** Se a base de dados local de uma instância falhar, ela pode atuar como *proxy* para uma instância saudável.

Implicações
-----------
- **Latência:** O pedido pode demorar mais se tiver de percorrer a lista de peers.
- **Dependência de Rede:** Aumenta o tráfego interno ("East-West traffic") entre contentores em cenários de falha.
- **Resiliência:** Implementação de `ResilientRestTemplate` para evitar chamar peers que estejam comprovadamente em baixo (*Circuit Breaker* simplificado).

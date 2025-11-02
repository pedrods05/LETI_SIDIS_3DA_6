# Documentação do Projeto HAP

Este índice facilita ao docente encontrar rapidamente os artefactos de documentação (diagramas C4), apontando diretamente para os ficheiros PUML (e para SVG quando existir).

- [C1 — System Context](#c1--system-context)
- [C2 — Containers](#c2--containers)
- [C3 — Logical View](#c3--logical-view)
- [C4 — Components](#c4--components)
- [C4+1 — Sequence Diagrams (SD)](#c41--sequence-diagrams-sd)
- [Como visualizar os diagramas](#como-visualizar-os-diagramas)

---

## C1 — System Context

| Diagrama | PUML | SVG |
|---|---|---|
| C1 – System Context Diagram | [Abrir PUML](./C1/C1-SystemContextDiagram.puml) | [Abrir SVG](./C1/C1-SystemContextDiagram.svg) |

---

## C2 — Containers

| Diagrama | PUML | SVG |
|---|---|---|
| C2 – Containers | [Abrir PUML](./C2/C2-Containers.puml) | [Abrir SVG](./C2/C2-Containers.svg) |

> Mostra os quatro containers principais (Physicians, Patients, Auth e AppointmentRecords) e as comunicações HTTP/REST entre eles.

---

## C3 — Logical View

| Diagrama | PUML | SVG |
|---|---|---|
| C3 – Logical View | [Abrir PUML](./C3/C3-LogicalView.puml) | [Abrir SVG](./C3/C3-LogicalView.svg) |

---

## C4 — Components

| Diagrama | PUML | SVG |
|---|---|---|
| (a preencher) | (a preencher) | (a preencher) |

---

## C4+1 — Sequence Diagrams (SD)

| Cenário | PUML | SVG |
|---|---|---|
| Login — POST /api/public/login | [PUML](./C4+1/C4+1-Login-Sequence.puml) | [SVG](./C4+1/C4+1-Login-Sequence.svg) |
| Patient Registration — POST /api/v2/patients/register | [PUML](./C4+1/C4+1-PatientRegistration-Sequence.puml) | [SVG](./C4+1/C4+1-PatientRegistration-Sequence.svg) |
| Appointment create — POST /appointments | [PUML](./C4+1/C4+1-AppointmentCreate-Sequence.puml) | [SVG](./C4+1/C4+1-AppointmentCreate-Sequence.svg) |
| Appointment local fetch — GET /appointments/{id} | [PUML](./C4+1/C4+1-AppointmentLocalFetch-Sequence.puml) | [SVG](./C4+1/C4+1-AppointmentLocalFetch-Sequence.svg) |
| Appointment peer forwarding — GET /appointments/{id} | [PUML](./C4+1/C4+1-AppointmentPeerForwarding-Sequence.puml) | [SVG](./C4+1/C4+1-AppointmentPeerForwarding-Sequence.svg) |
| Patient peer forwarding — GET /patients/{id} | [PUML](./C4+1/C4+1-PatientPeerForwarding-Sequence.puml) | [SVG](./C4+1/C4+1-PatientPeerForwarding-Sequence.svg) |
| Appointment update — PUT /appointments/{id} | [PUML](./C4+1/C4+1-AppointmentUpdate-Sequence.puml) | [SVG](./C4+1/C4+1-AppointmentUpdate-Sequence.svg) |
| Appointment cancel — PUT /appointments/{id}/cancel | [PUML](./C4+1/C4+1-AppointmentCancel-Sequence.puml) | [SVG](./C4+1/C4+1-AppointmentCancel-Sequence.svg) |
| Get Patient Details (Admin) — GET /patients/{id} | [PUML](./C4+1/C4+1-GetPatientDetails-Sequence.puml) | [SVG](./C4+1/C4+1-GetPatientDetails-Sequence.svg) |

---

## Como visualizar os diagramas

- No IDE (JetBrains):
  - Abra o ficheiro `.puml` e utilize a opção de Pré‑visualização PlantUML (plugin "PlantUML Integration" ou equivalente).
  - Em modo preview, o diagrama é renderizado automaticamente a partir do texto PUML.
- Exportação para imagem (opcional):
  - A partir do preview, poderá exportar para PNG/SVG conforme as opções do plugin. Se exportar os SVG para as mesmas pastas dos PUML, os links acima poderão ser atualizados para apontar também para os SVG.

---

Pasta base da documentação: [DOCS](./)

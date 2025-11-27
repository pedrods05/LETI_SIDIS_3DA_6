# ADR 0003: Use MongoDB for AppointmentRecords

Context
- Requirement: mandatory use of MongoDB for at least one database.
- Appointment scheduling aggregates fit a document model with embedded status history and notes.

Decision
- Use MongoDB as the primary persistence for `hap-appointmentrecords`.
- Model each Appointment as a single document; include status, time range, participants, status history, and notes.

Consequences
- Efficient reads/writes for aggregate operations; minimal need for multi-document transactions.
- Define indexes:
  - `{ physicianId, start, end, status }` to prevent overlaps.
  - `{ patientId, start }` for patient queries.
- Use Spring Data MongoDB repository interfaces; consider reactive (`ReactiveMongoRepository`) if WebFlux is chosen.


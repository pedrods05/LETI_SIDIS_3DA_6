# ADR 0002: Microservice Boundaries and Independence

Context
- Current `hap-appointmentrecords` module declares compile-time dependencies on `hap-auth`, `hap-patients`, and `hap-physicians`.
- The goal is independent microservices with explicit APIs and no artifact coupling.

Decision
- Remove compile-time dependencies between services. Communication is via REST APIs using HTTP, with typed DTOs defined within each service.
- Each service owns its data store and domain model; no shared persistence.

Consequences
- AppointmentRecords will use HTTP clients (e.g., Spring WebClient/RestTemplate) to validate Patient/Physician identifiers.
- Enables independent builds, deployments, and scaling; avoids tight coupling and classpath conflicts.


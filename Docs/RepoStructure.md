# Repository Structure Overview

This document explains, directory by directory and file by file, how this project is organized and what each part is responsible for.

Top-level (aggregator):
- `pom.xml`: Maven aggregator and shared dependency management for all microservices. Sets Java 17, Spring Boot 3.5.6, common dependencies (web, security, JPA, validation, springdoc), and build plugins.
- `Dockerfile`: Containerization base for building/running an app (content to be refined per service or a top-level image).
- `mvnw`, `mvnw.cmd`: Maven wrapper scripts for consistent builds.
- `Docs/`: Project documentation (Domain Model, Use Cases, Glossary, FURPS, SwaggerGuide, Work Packages). New docs will be added here (ADRs, C4 diagrams, OpenAPI specs).
- `src/`: Placeholder for a monolith or shared examples (currently not used as a deployable service).
- `photos/`: Static assets.

Microservice modules:

1) `hap-auth/`
- `pom.xml`: Spring Boot microservice for Authentication (web, security, JPA, validation, springdoc, H2 dev). JWT/OAuth2 resource server.
- `compose.yaml`: Docker Compose for local development of the auth service (to verify).
- `src/main/java`: Java sources (controllers, services, repositories, domain). Typical packages: `controller`, `service`, `repository`, `domain`, `config`.
- `src/main/resources`: Config files (e.g., `application.yml`).
- `src/test/java`: Tests (unit/integration).

2) `hap-patients/`
- `pom.xml`: Patients microservice (web, security, JPA, validation, retry/aop, springdoc, H2 dev). Explicitly decoupled from `hap-auth` at compile-time.
- `compose.yaml`: Docker Compose for local development (to verify).
- `src/main/java`, `resources`, `test`: Code and configuration for patient profiles APIs.

3) `hap-physicians/`
- `pom.xml`: Physicians microservice (web, security, JPA, validation, springdoc, H2 dev). Includes retry/aspects and JWT/OAuth2 resource support.
- `README.md`: Module-specific notes.
- `compose.yaml`: Docker Compose for local development (to verify).
- `src/main/java`, `resources`, `test`: Code and configuration for physician profiles and availability.

4) `hap-appointmentrecords/`
- `pom.xml`: AppointmentRecords microservice. Includes JPA/JDBC and MongoDB (both blocking and reactive drivers), WebMVC and WebFlux, OAuth2 auth server/client/resource, Testcontainers for MongoDB and other DBs, and Asciidoctor plugin. Currently has compile-time dependencies on other modules (to be removed for independence).
- `README.md`: Module-specific notes.
- `compose.yaml`: Docker Compose for local development; will be updated to include MongoDB.
- `src/main/java`, `resources`, `test`: Code and configuration for scheduling and appointment lifecycle.

Existing documentation:
- `Docs/DM.puml`: Domain model.
- `Docs/UCD.md`: Use case diagrams.
- `Docs/SwaggerGuide.md`: Swagger/OpenAPI guidance.
- Work Packages `Docs/WP#*/*`: User Stories, sequence diagrams.

Notes and next actions:
- We will add ADRs under `Docs/ADR/*`, C4 diagrams under `Docs/C4/*`, and per-service OpenAPI specs under `Docs/OpenAPI/*`.
- We will configure `hap-appointmentrecords` to use MongoDB as its primary persistence and remove compile-time coupling to other services.


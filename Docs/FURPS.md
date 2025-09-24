# Supplementary Specification (FURPS+)

## Functionality

_Specifies functionalities that:_

- _are common across several US/UC;_
- _are not related to US/UC, namely: Audit, Reporting and Security._

* Only administrators can register, update or delete physicians and patients.
* Patients can only view or edit their own appointments and information.
* Physicians can only view and create appointment records for their assigned appointments.
* Anonymous users may register as patients with optional profile photo and list of health concerns.
* Authentication via JWT is required for all authenticated roles (ADMIN, PHYSICIAN, PATIENT).
* Patients must have data consent registered to be scheduled for an appointment.
* The system must provide reports and analytics:
   - Top 5 physicians (by number of appointments).
   - Appointment statistics by patient age group.
   - Monthly reports: total, cancelled, and rescheduled appointments.
   - Average appointment duration per physician.
## Usability

_Evaluates the user interface. It has several subcategories,
among them: error prevention; interface aesthetics and design; help and
documentation; consistency and standards._

* The API must follow consistent naming and versioning conventions.
* Form validation and clear error messages must be supported (e.g., invalid phone or email).
* International phone formats and standard postal addresses must be accepted.
* Swagger UI must be available for endpoint documentation and testing.
* Time slots must be shown in a readable and ordered format to patients.
* Patient and physician profile photo upload must be supported using multipart/form-data.
## Reliability

* The system should be able to control concurrent access to information without losing any data.
* All operations that alter data must be transactional and rollback-safe.
* Token expiration must be validated on each request.
* Physicians must not be double-booked for overlapping time slots.
* All analytics must be based on persisted records (not just appointment status).

## Performance

_Evaluates the performance requirements of the software, namely: response time, start-up time, recovery time, memory consumption, CPU usage, load capacity and application availability._

* The system must respond to most API requests within 500ms under normal load.
* It must support at least 100 concurrent users without degradation.
* Application start-up time must not exceed 5 seconds in development.
* Monthly reporting endpoints must return results under 1 second for ≤10k records.

## Supportability

_The supportability requirements gather several characteristics, such as:
testability, adaptability, maintainability, compatibility,
configurability, installability, scalability and more._

* The system must be modular and layered to facilitate maintainability.
* It must be compatible with Java 17 and Spring Boot 3.x.
* Unit and integration tests must be written using JUnit and Postman.
* Different application profiles (dev, test, prod) must be supported.
* DTO mapping must be centralized using MapStruct to reduce boilerplate.

## +

### Design Constraints

_Specifies or constraints the system design process. Examples may include: programming languages, software process, mandatory standards/patterns, use of development tools, class library, etc._

* The system should follow a RESTful service architecture.
* The communication with the database must be done via the Java Persistence API (JPA).
* MapStruct must be used for model ↔ DTO conversion.
* Swagger/OpenAPI must be used to document endpoints.
* Separation of user roles (ADMIN, PATIENT, PHYSICIAN) must be enforced through Spring Security + annotations.
*
Appointment availability must be calculated based on physician schedule and existing bookings.

### Implementation Constraints

_Specifies or constraints the code or construction of a system such as: mandatory standards/patterns, implementation languages,
database integrity, resource limits, operating system._

* The system must be developed using Java 17.
* Maven must be used for project and dependency management.
* Passwords must be encrypted using BCrypt.
* Use UUID or prefixed IDs (e.g., PHY01, PAT01) for domain entities for readability.

### Interface Constraints

_Specifies or constraints the features inherent to the interaction of the
system being developed with other external systems._

* All communication must be done using REST and JSON.
* Endpoints must use appropriate HTTP methods and status codes.
* The API must provide CORS support for frontend integration.

### Physical Constraints

_Specifies a limitation or physical requirement regarding the hardware used to house the system, as for example: material, shape, size or weight._

* The system must run on any standard Linux-based server.
* Image storage and database must be configurable via environment variables.

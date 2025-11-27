# ADR 0001: DDD Bounded Contexts for HAP
  - Optional async events (future): Publish domain events for eventual consistency.
  - Synchronous REST (reads): AppointmentRecords validates patient/physician existence via dedicated endpoints.
- Interaction patterns:
- AppointmentRecords does not store patient/physician PII; it only references IDs.
- Clear service boundaries and data ownership; each context will be implemented as an independent microservice.
Consequences

  4. AppointmentRecords Context: Scheduling and lifecycle of appointments; references PatientId and PhysicianId; responsibility for enforcing scheduling rules.
  3. Physicians Context: Physician profiles, specialties, availability templates; responsibility for physician identity and availability definition.
  2. Patients Context: Patient profiles, demographics, contacts; responsibility for patient identity and profile lifecycle.
  1. Auth Context: Users, Roles, Tokens; responsibility for authentication/authorization.
- Adopt Domain-Driven Design with four bounded contexts:
Decision

- The repository already includes modules: `hap-auth`, `hap-patients`, `hap-physicians`, `hap-appointmentrecords`.
- The HAP project manages authentication, patient profiles, physician profiles, and scheduling of appointments.
Context



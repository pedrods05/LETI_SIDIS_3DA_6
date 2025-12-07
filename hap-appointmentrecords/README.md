# HAP-APPOINTMENTRECORDS - Technical Reference Manual


---

## Table of Contents
1. [Architecture and Design (DDD/CQRS)](#1-architecture-and-design-dddcqrs)
2. [Project Structure and Technologies](#2-project-structure-and-technologies)
3. [Database Per Instance Configuration](#3-database-per-instance-configuration)
4. [Execution and Installation Guide](#4-execution-and-installation-guide)
5. [Detailed Testing Roadmap (HTTP & AMQP)](#5-detailed-testing-roadmap-http--amqp)
6. [Troubleshooting and Common Errors](#6-troubleshooting-and-common-errors)
7. [Defense Guide (Q&A)](#7-defense-guide-qa)

---

## 1. Architecture and Design (DDD/CQRS)

### Overview
The `hap-appointmentrecords` module is a microservice focused exclusively on managing **Clinical Records** post-consultation. It acts as a **Passive Participant** in the system choreography.

### Communication Diagram
```ascii
┌─────────────────────────┐
│   hap-physicians        │ ← Appointment Owner (Single Source of Truth)
│   (port 8081)           │
└──────────┬──────────────┘
           │
           │ HTTP Queries (Synchronous + Circuit Breaker)
           │ "GET /appointments/{id}"
           ▼
┌─────────────────────────┐      ┌─────────────────────────┐
│ hap-appointmentrecords  │◄─────│  RabbitMQ (Topic)       │
│                         │      │  Exchange: hap-exchange │
│    AppointmentRecord    │      │  Key: appointment.* │
│    (Write: H2)          │      └─────────────────────────┘
│                         │
│    RecordProjection     │◄─────┐
│    (Read: MongoDB)      │      │
└─────────────────────────┘      │
                                 │
                          ┌──────┴───────┐
                          │   MongoDB     │
                          │  (Record      │
                          │   Projections)│
                          └──────────────┘
```

### Refactoring Decisions
* **Removal of Duplicate Data:** The `Appointment` entity was removed from this service. We now store only the `appointmentId` (String).
* **Synchronous Communication:** Detailed patient/physician data is retrieved in real-time via `ExternalServiceClient` (HTTP) from the `hap-physicians` service.
* **Strict CQRS:**
    * **Command (Write):** H2 (Transactional, ensures integrity).
    * **Query (Read):** MongoDB (Denormalized for read performance).

---

## 2. Project Structure and Technologies

### Tech Stack
* **Core:** Spring Boot 3.5.6, Java 17+
* **Data:** Spring Data JPA (H2), Spring Data MongoDB
* **Messaging:** Spring AMQP (RabbitMQ) with `Jackson2JsonMessageConverter`
* **Resilience:** Resilience4j (Circuit Breaker for HTTP calls)

### Key Classes (Post-Refactoring)
* `AppointmentRecord` - JPA Entity (Write Model).
* `AppointmentRecordProjection` - Mongo Document (Read Model).
* `ExternalServiceClient` - HTTP Client to communicate with `hap-physicians`.
* `AppointmentEventsListener` - RabbitMQ Consumer (Logging/Tracing only).
* `InternalAppointmentRecordController` - Controller for Peer Forwarding.

---

## 3. Database Per Instance Configuration

Each instance operates in isolation, simulating distinct physical servers.

| Configuration | Instance 1 | Instance 2 |
| :--- | :--- | :--- |
| **Port** | `8083` | `8090` |
| **Profile** | `instance1` | `instance2` |
| **MongoDB URI** | `.../hapappointmentrecords_instance1` | `.../hapappointmentrecords_instance2` |
| **H2 URL** | `jdbc:h2:mem:instance1db` | `jdbc:h2:mem:instance2db` |
| **Peer Target** | `http://localhost:8090` | `http://localhost:8083` |

**Peer Forwarding:** If Instance 1 receives a request for a record it does not possess, it internally forwards the request to Instance 2 via HTTP, ensuring transparency for the client.

---

## 4. Execution and Installation Guide

### Prerequisites
* Docker Desktop running.
* Java 17 installed and configured in PATH.

### Step 1: Support Infrastructure
```bash
# In the project root (where docker-compose.yml is located)
docker compose up -d mongodb rabbitmq
```

### Step 2: Start Instances
Open two terminals in the `hap-appointmentrecords` folder:

**Terminal 1 (Instance 1):**
```bash
.\start-instance1.bat
```

**Terminal 2 (Instance 2):**
```bash
.\start-instance2.bat
```
*(Note: The script uses `mvnw spring-boot:run` with specific profiles)*

---

## 5. Detailed Testing Roadmap (HTTP & AMQP)

### Test A: Data Isolation (Database per Instance)

**1. Create Record on Instance 1 (8083)**
```http
POST http://localhost:8083/api/appointment-records/APT-001/record
Content-Type: application/json

{
  "diagnosis": "Instance 1 Test",
  "treatmentRecommendations": "Rest",
  "prescriptions": "Paracetamol",
  "duration": "00:30:00"
}
```
> **Validation:** Verify in MongoDB Compass or Shell that database `hapappointmentrecords_instance1` contains the record and `...instance2` does **NOT**.

### Test B: Peer Forwarding

**1. Read on Instance 2 (8090) the record created on Instance 1**
* Get the `recordId` returned in the previous step (e.g., `REC-123`).
* Execute:
```http
GET http://localhost:8090/api/appointment-records/REC-123
```
> **Validation:** Should receive `200 OK`. Instance 2 logs should show: `Querying peer: http://localhost:8083...`.

### Test C: RabbitMQ Integration (Smoke Test)

To verify if the listener is receiving events (check logs):

**Example JSON Payload (AppointmentCreatedEvent):**
```json
{
  "appointmentId": "apt-rabbit-01",
  "patientId": "p1",
  "physicianId": "d1",
  "dateTime": "2025-12-10T09:00:00",
  "consultationType": "FIRST_TIME",
  "status": "SCHEDULED",
  "occurredAt": "2025-12-05T10:00:00"
}
```

**PowerShell Command to publish to Exchange:**
```powershell
$payload = '{"appointmentId":"apt-rabbit-01", ... (json above) ... }'
$body = @{ properties = @{}; routing_key = 'appointment.created'; payload = $payload; payload_encoding = 'string' } | ConvertTo-Json
Invoke-RestMethod -Uri 'http://localhost:15672/api/exchanges/%2F/hap-exchange/publish' -Method Post -Body $body -ContentType 'application/json' -Credential (Get-Credential)
```
> **Validation:** Check application logs. It should appear: `Event AppointmentCreatedEvent received | appointmentId=apt-rabbit-01`.

---

## 6. Troubleshooting and Common Errors

### "Connection refused" on startup
* **Cause:** MongoDB or RabbitMQ are not accessible.
* **Solution:** Check if Docker containers are UP (`docker ps`). Confirm ports 27017 and 5672.

### "Peer request failed" or 404 on Cross-Read
* **Cause:** The other instance is not running or the peer URL is wrong.
* **Solution:** Confirm both terminal windows are open and error-free. Check `application.properties` for each profile to confirm `hap.appointmentrecords.peers`.

### Maven Build Errors on Windows
* **Error:** "mvnw is not recognized..."
* **Solution:** Use `.\mvnw.cmd` or install Maven globally and use `mvn`. If there are spaces in the folder path, wrap the path in quotes.

### Logs show "CircuitBreaker 'externalService' is OPEN"
* **Cause:** The `hap-physicians` service (port 8081) is down and the application tried to contact it repeatedly.
* **Solution:** Start the `hap-physicians` service or wait for the Circuit Breaker to switch to `HALF_OPEN` state (retries after a few seconds).

---

## 7. Defense Guide (Q&A)

**Q: Is the system resilient?**
A: "Yes. At the data level, instance isolation prevents cascading corruption. At the communication level, we use **Circuit Breakers** on HTTP calls to the physician service, ensuring our service doesn't block if theirs fails."

**Q: Why did you remove the local Appointment table?**
A: "To respect the **Single Source of Truth**. Keeping copies of data from other services creates complex synchronization issues (eventual consistency). Reference by ID + real-time query is more robust for this use case."

**Q: What happens if RabbitMQ goes down?**
A: "Since we are **passive participants** (we use events only for logging/tracing), the core functionality of the system (creating and reading clinical records) **continues to work at 100%**. We only lose real-time observability of scheduling events."
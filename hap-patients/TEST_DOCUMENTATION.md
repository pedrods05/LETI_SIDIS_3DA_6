# Test Documentation - hap-patients Module

## Overview
This document provides documentation of the main unit and integration tests in the hap-patients module, organized by functionality and architectural patterns.

**Last Updated:** December 8, 2025  
**Total Test Classes:** 15  
**Test Coverage Areas:** API Controllers, Services, Repositories, Events, Configuration, Models, Integration

---

## Table of Contents
1. [API Layer Tests](#api-layer-tests)
2. [Service Layer Tests](#service-layer-tests)
3. [Repository Layer Tests](#repository-layer-tests)
4. [Event Handling Tests](#event-handling-tests)
5. [Configuration / HTTP Client Tests](#configuration--http-client-tests)
6. [Model / Mapping Tests](#model--mapping-tests)
7. [Exception Handling Tests](#exception-handling-tests)
8. [Integration Tests](#integration-tests)
9. [Running the Tests](#running-the-tests)

---

## API Layer Tests

### 1. PatientControllerTest
**File:** `src/test/java/leti_sisdis_6/happatients/api/PatientControllerTest.java`

**Purpose:** Tests the main patient endpoints exposed to external consumers.

**Typical Scenarios Covered:**
- Successful retrieval of a patient by id
- Behaviour when the patient does not exist (404)
- Use of the local store vs. peer calls for patient details
- Response structure (DTO fields) and HTTP status codes for the happy path and error paths

**Key Concerns:**
- Correct HTTP status codes (200, 404, 5xx)
- Use of `MockMvc` with security filter chain
- Delegation to the service / repository layer instead of duplicating business logic

---

### 2. PatientControllerPeerForwardingTest
**File:** `src/test/java/leti_sisdis_6/happatients/api/PatientControllerPeerForwardingTest.java`

**Purpose:** Focuses on peer-forwarding behaviour when a patient is not found locally.

**Typical Scenarios Covered:**
- When a patient is not found locally, the controller should call a configured peer and return the peer result
- When peers fail or return not-found, the controller should return the appropriate error code (e.g. 404)
- When a patient exists locally, no peer call should be made

**Key Concerns:**
- HTTP status codes for peer scenarios (200, 400, 404)
- Verification that the peer HTTP client is invoked or not invoked as expected

---

### 3. InternalPatientControllerTest
**File:** `src/test/java/leti_sisdis_6/happatients/api/InternalPatientControllerTest.java`

**Purpose:** Tests internal endpoints used for inter-service / peer communication.

**Typical Scenarios Covered:**
- Successful internal retrieval of a patient (`/internal/...` style endpoint)
- Not-found behaviour for the internal endpoint
- Security configuration for internal endpoints (typically more permissive or white‑listed)

---

### 4. PatientRegistrationControllerTest
**File:** `src/test/java/leti_sisdis_6/happatients/api/PatientRegistrationControllerTest.java`

**Purpose:** Tests HTTP endpoints responsible for registering new patients.

**Typical Scenarios Covered:**
- Successful registration with valid input data
- Validation errors (missing required fields, invalid formats) and resulting 400 responses
- Interaction with the registration service (command side / CQRS)

---

## Service Layer Tests

### 5. PatientServiceTest
**File:** `src/test/java/leti_sisdis_6/happatients/service/PatientServiceTest.java`

**Purpose:** Tests core read‑side patient business logic.

**Typical Scenarios Covered:**
- Getting patient details when they exist locally
- Behaviour when the patient does not exist
- Basic mapping from entities to DTOs

---

### 6. PatientRegistrationServiceTest
**File:** `src/test/java/leti_sisdis_6/happatients/service/PatientRegistrationServiceTest.java`

**Purpose:** Tests the patient registration business logic, including persistence and integration with other services.

**Typical Scenarios Covered:**
- Successful registration flow persisting entities and invoking external services (e.g. auth)
- Handling of duplicate data (such as email) and conflict behaviour
- Event publishing (e.g. RabbitMQ) after registration

---

## Repository Layer Tests

### 7. PatientRepositoryTest
**File:** `src/test/java/leti_sisdis_6/happatients/repository/PatientRepositoryTest.java`

**Purpose:** Tests the JPA repository used for patient write‑model persistence.

**Typical Scenarios Covered:**
- Saving and loading patients via JPA/H2
- Custom query methods (e.g. find by patient id or email)

---

### 8. PatientLocalRepositoryTest
**File:** `src/test/java/leti_sisdis_6/happatients/repository/PatientLocalRepositoryTest.java`

**Purpose:** Tests local/peer lookup logic encapsulated in the local repository abstraction.

**Typical Scenarios Covered:**
- Preference for local data over remote peers
- Behaviour when peers are consulted and when all peers fail / return not-found

---

## Event Handling Tests

### 9. PatientEventHandlerTest
**File:** `src/test/java/leti_sisdis_6/happatients/event/PatientEventHandlerTest.java`

**Purpose:** Tests the RabbitMQ event consumer that updates the read‑side model.

**Typical Scenarios Covered:**
- Handling of a patient‑registered event and writing a summary document
- Behaviour when headers such as correlation id are present or missing

---

### 10. PatientRegisteredEventTest
**File:** `src/test/java/leti_sisdis_6/happatients/event/PatientRegisteredEventTest.java`

**Purpose:** Tests the event DTO that represents a patient registration in the messaging layer.

**Typical Scenarios Covered:**
- Building events with all relevant fields set
- Basic (de)serialisation and equality/field behaviour

---

## Configuration / HTTP Client Tests

### 11. RabbitMQConfigTest
**File:** `src/test/java/leti_sisdis_6/happatients/config/RabbitMQConfigTest.java`

**Purpose:** Tests configuration related to RabbitMQ (exchanges, queues, converters, templates).

**Typical Scenarios Covered:**
- Creation of topic exchange and message converter beans
- Proper configuration of the `RabbitTemplate` (e.g. message converter, pre‑publish post‑processor)

---

### 12. ResilientRestTemplateTest
**File:** `src/test/java/leti_sisdis_6/happatients/http/ResilientRestTemplateTest.java`

**Purpose:** Tests the HTTP client / RestTemplate configuration for inter‑service calls.

**Typical Scenarios Covered:**
- Presence of interceptors for adding headers (e.g. auth token)
- Basic timeout / error handling configuration

---

## Model / Mapping Tests

### 13. PatientSummaryTest
**File:** `src/test/java/leti_sisdis_6/happatients/query/PatientSummaryTest.java`

**Purpose:** Tests the MongoDB read‑model document used for patient summaries.

**Typical Scenarios Covered:**
- Creation of summary objects with nested value types (e.g. address/insurance)
- Getter/setter / Lombok behaviour

---

### 14. PatientMapperTest
**File:** `src/test/java/leti_sisdis_6/happatients/api/PatientMapperTest.java`

**Purpose:** Tests mapping between domain entities and API DTOs.

**Typical Scenarios Covered:**
- Entity → DTO mapping for patient details/profile
- Handling of null or optional fields during mapping

---

## Exception Handling Tests

### GlobalExceptionHandlerTest
**File:** `src/test/java/leti_sisdis_6/happatients/exceptions/GlobalExceptionHandlerTest.java`

**Purpose:** Tests the `@ControllerAdvice` that centralises error handling for the module.

**Typical Scenarios Covered:**
- Validation errors translated to HTTP 400 with structured error information
- Not‑found and conflict scenarios mapped to 404 and 409
- Generic exception handling for unexpected errors

---

## Integration Tests

### HapPatientsApplicationTests
**File:** `src/test/java/leti_sisdis_6/happatients/HapPatientsApplicationTests.java`

**Purpose:** Spring Boot context loading test to ensure the module can start with its configuration and infrastructure components.

**Typical Scenarios Covered:**
- `contextLoads` – verifies that the Spring application context starts without failing bean creation

---

## Running the Tests

From the root of the `hap-patients` module you can run all tests with Maven:

```bash
mvn test
```

### Run Tests in IntelliJ
- Right-click on `src/test/java` → **Run 'All Tests'**
- Right-click on a specific test class → **Run 'ClassNameTest'**
- Use `Ctrl+Shift+F10` to run the test at cursor

---


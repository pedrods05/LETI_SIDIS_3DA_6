# Test Documentation - hap-physicians Module

## Overview
This document provides documentation of the main unit and integration tests in the hap-physicians module, organized by functionality and architectural patterns.

**Last Updated:** December 8, 2025  
**Total Test Classes:** 12  
**Test Coverage Areas:** API Controllers, Services, Repositories, Events, Configuration, Models, Utilities, Integration

---

## Table of Contents
1. [API Layer Tests](#api-layer-tests)
2. [Service Layer Tests](#service-layer-tests)
3. [HTTP Client Tests](#http-client-tests)
4. [Model / DTO Tests](#model--dto-tests)
5. [Utility Tests](#utility-tests)
6. [Exception Handling Tests](#exception-handling-tests)
7. [Running the Tests](#running-the-tests)

---

## API Layer Tests

### 1. PhysicianControllerTest
**File:** `src/test/java/leti_sisdis_6/happhysicians/api/PhysicianControllerTest.java`

**Purpose:** Tests the main physician endpoints exposed to external consumers.

**Typical Scenarios Covered:**
- Successful retrieval of a physician by id
- Successful registration of a new physician
- Successful update of physician information
- Calculation and retrieval of available appointment slots
- Behaviour when the physician does not exist (404)
- Response structure (DTO fields) and HTTP status codes for the happy path and error paths
- Integration with command and query services (CQRS pattern)

**Key Concerns:**
- Correct HTTP status codes (200, 201, 404, 5xx)
- Delegation to the service / command / query layer instead of duplicating business logic
- Proper use of CQRS pattern (commands for writes, queries for reads)

---

### 2. AppointmentControllerTest
**File:** `src/test/java/leti_sisdis_6/happhysicians/api/AppointmentControllerTest.java`

**Purpose:** Tests HTTP endpoints responsible for managing appointments (scheduling, updating, canceling, retrieving).

**Typical Scenarios Covered:**
- Successful creation of an appointment
- Successful retrieval of appointment details
- Successful update of appointment information
- Successful cancellation of an appointment
- Retrieval of audit trail for an appointment (Event Sourcing)
- Validation errors (missing required fields, invalid formats) and resulting 400 responses
- Interaction with command and query services (CQRS pattern)
- Integration with EventStoreService for audit trail

**Key Concerns:**
- Correct HTTP status codes (200, 201, 400, 404, 5xx)
- Proper use of CQRS pattern (commands for writes, queries for reads)
- Event sourcing integration for audit trail functionality

---

## Service Layer Tests

### 3. PhysicianServiceTest
**File:** `src/test/java/leti_sisdis_6/happhysicians/services/PhysicianServiceTest.java`

**Purpose:** Tests core physician business logic including registration, retrieval, and updates.

**Typical Scenarios Covered:**
- Getting physician details when they exist
- Registering a new physician with valid data
- Updating physician information
- Searching physicians by name or specialty
- Behaviour when the physician does not exist
- Integration with repositories and external services

---

### 4. AppointmentServiceTest
**File:** `src/test/java/leti_sisdis_6/happhysicians/services/AppointmentServiceTest.java`

**Purpose:** Tests the appointment business logic, including scheduling, validation, and integration with other services.

**Typical Scenarios Covered:**
- Successful appointment creation flow with validations
- Appointment retrieval with patient and record details
- Appointment updates and cancellations
- Handling of time slot conflicts
- Integration with external services (patients, appointment records)
- Validation of appointment time slots against physician working hours
- Handling of appointment status transitions

**Key Concerns:**
- Business rule validation (time slots, physician availability)
- Integration with ExternalServiceClient for cross-service communication
- Proper error handling for external service failures

---

### 5. ExternalServiceClientTest
**File:** `src/test/java/leti_sisdis_6/happhysicians/services/ExternalServiceClientTest.java`

**Purpose:** Tests the HTTP client used for inter-service communication with hap-patients, hap-auth, and hap-appointmentrecords.

**Typical Scenarios Covered:**
- Successful retrieval of patient data from hap-patients
- Successful retrieval of appointment records from hap-appointmentrecords
- User registration and validation with hap-auth
- Peer URL configuration and retrieval
- Error handling for external service failures
- Circuit breaker behaviour (when applicable)

**Key Concerns:**
- Correct HTTP calls to external services
- Proper error handling and exception translation
- Resilience patterns (circuit breaker, retries)

---

## HTTP Client Tests

### 6. ResilientRestTemplateTest
**File:** `src/test/java/leti_sisdis_6/happhysicians/http/ResilientRestTemplateTest.java`

**Purpose:** Tests the HTTP client / RestTemplate configuration for inter‑service calls.

**Typical Scenarios Covered:**
- Presence of interceptors for adding headers (e.g. auth token, correlation ID)
- Basic timeout / error handling configuration
- Retry logic for transient failures

---

## Model / DTO Tests

### 7. AppointmentDetailsDTOTest
**File:** `src/test/java/leti_sisdis_6/happhysicians/dto/output/AppointmentDetailsDTOTest.java`

**Purpose:** Tests the DTO used for returning appointment details in API responses.

**Typical Scenarios Covered:**
- Creation of DTO objects with all relevant fields
- Getter/setter / Lombok behaviour
- Mapping from domain entities to DTOs

---

### 8. ConsultationTypeTest
**File:** `src/test/java/leti_sisdis_6/happhysicians/model/ConsultationTypeTest.java`

**Purpose:** Tests the ConsultationType enum used to categorize appointments.

**Typical Scenarios Covered:**
- Enum value validation
- String conversion and parsing

---

### 9. AppointmentStatusTest
**File:** `src/test/java/leti_sisdis_6/happhysicians/model/AppointmentStatusTest.java`

**Purpose:** Tests the AppointmentStatus enum used to track appointment lifecycle.

**Typical Scenarios Covered:**
- Enum value validation
- Status transition validation
- String conversion and parsing

---

## Utility Tests

### 10. AppointmentTimeValidatorTest
**File:** `src/test/java/leti_sisdis_6/happhysicians/util/AppointmentTimeValidatorTest.java`

**Purpose:** Tests the utility class responsible for validating appointment time slots.

**Typical Scenarios Covered:**
- Validation of time slots within physician working hours
- Detection of overlapping appointments
- Validation of appointment times in the future
- Handling of edge cases (boundary times, timezone considerations)

---

### 11. SlotCalculatorTest
**File:** `src/test/java/leti_sisdis_6/happhysicians/util/SlotCalculatorTest.java`

**Purpose:** Tests the utility class responsible for calculating available appointment slots.

**Typical Scenarios Covered:**
- Calculation of available slots based on physician working hours
- Exclusion of already booked time slots
- Handling of different slot durations
- Edge cases (no available slots, all slots available)

---

## Exception Handling Tests

### 12. GlobalExceptionHandlerTest
**File:** `src/test/java/leti_sisdis_6/happhysicians/exceptions/GlobalExceptionHandlerTest.java`

**Purpose:** Tests the `@ControllerAdvice` that centralises error handling for the module.

**Typical Scenarios Covered:**
- Validation errors translated to HTTP 400 with structured error information
- Not‑found scenarios mapped to 404
- Conflict scenarios mapped to 409
- External service communication errors mapped to appropriate status codes
- Generic exception handling for unexpected errors

---

## Running the Tests

From the root of the `hap-physicians` module you can run all tests with Maven:

```bash
mvn test
```

### Run Tests in IntelliJ
- Right-click on `src/test/java` → **Run 'All Tests'**
- Right-click on a specific test class → **Run 'ClassNameTest'**
- Use `Ctrl+Shift+F10` to run the test at cursor

### Run Specific Test Classes
```bash
mvn test -Dtest=PhysicianControllerTest
mvn test -Dtest=AppointmentServiceTest
mvn test -Dtest=ExternalServiceClientTest
```

---

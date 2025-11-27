# HAP Platform - Architecture & Design Decisions (Assignment 2)

Este documento regista as principais decisões arquiteturais tomadas durante a migração da plataforma HAP para uma arquitetura distribuída e orientada a eventos.

---

## 1. Padrão Arquitetural: CQRS (Command Query Responsibility Segregation)
Write Side (Commands): Responsável pela validação de regras de negócio e persistência transacional
- POST /api/public/register → Command: RegisterUser
- POST /api/public/login → Command: AuthenticateUser
- POST /api/v2/patients/register → Command: RegisterPatient
- POST /physicians/register → Command: RegisterPhysician
- POST /appointments → Command: CreateAppointment
- PUT /appointments/{id} → Command: UpdateAppointment
- PUT /appointments/{id}/cancel → Command: CancelAppointment
- POST /api/appointment-records/{id}/record → Command: CreateAppointmentRecord
Read Side (Queries): Responsável por servir dados rapidamente ao cliente, utilizando projeções de dados desnormalizadas.
- GET /patients/{id} → Query: GetPatientById
- GET /physicians/{id} → Query: GetPhysicianById
- GET /appointments → Query: GetAllAppointments
- GET /appointments/upcoming → Query: GetUpcomingAppointments
- GET /api/appointment-records/{id} → Query: GetAppointmentRecordById
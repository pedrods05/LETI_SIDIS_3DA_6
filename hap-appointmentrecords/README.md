# HAP-APPOINTMENTRECORDS

## Overview

O módulo HAP-AppointmentRecords é responsável pela gestão de registos de consultas no sistema hospitalar. Este módulo funciona como um microserviço independente que permite a criação e visualização de registos de consultas, comunicando com outros módulos via HTTP/REST.

## Tecnologias Utilizadas

- Spring Boot 3.5.6
- Spring Data JPA
- Spring Security
- Spring Web (RestTemplate)
- H2 Database (in-memory)
- Lombok
- Maven

## Comunicação Inter-Serviços

Este módulo implementa comunicação HTTP/REST com outros microserviços seguindo o padrão estabelecido no projeto:

### Serviços Comunicados

1. **hap-physicians** (porta 8081) - Para obter dados de médicos e consultas
2. **hap-patients** (porta 8082) - Para obter dados de pacientes
3. **hap-auth** (porta 8084) - Para validação de tokens

### Configuração HTTP

A comunicação é configurada através de:

- `HttpClientConfig.java` - Configuração do RestTemplate com timeouts e interceptors
- `ExternalServiceClient.java` - Cliente para comunicação com serviços externos
- URLs configuradas em `application.properties`

### Exemplo de Uso

```java
@Service
public class AppointmentRecordService {
    @Autowired
    private ExternalServiceClient externalServiceClient;

    public AppointmentRecordResponse createRecord(String appointmentId, AppointmentRecordRequest request, String physicianId) {
        // Obter dados da consulta do serviço hap-physicians via HTTP
        Map<String, Object> appointmentData = externalServiceClient.getAppointmentById(appointmentId);
        
        // Verificar autorização
        String appointmentPhysicianId = (String) appointmentData.get("physicianId");
        if (!appointmentPhysicianId.equals(physicianId)) {
            throw new UnauthorizedException("Not authorized");
        }
        
        // Criar registo local
        // ...
    }
}
```

### Comunicação Pura HTTP/REST
O módulo **NÃO** importa classes de outros módulos diretamente. Toda comunicação é feita via HTTP/REST:
- **Dados de médicos**: Obtidos via `GET /physicians/{id}` do hap-physicians
- **Dados de consultas**: Obtidos via `GET /appointments/{id}` do hap-physicians  
- **Dados de pacientes**: Obtidos via `GET /patients/{id}` do hap-patients
- **DTOs locais**: Usa `UserDTO` local em vez de importar `User` e `Role` do hap-auth

### Endpoints de Comunicação

#### hap-physicians
- `GET /physicians/{physicianId}` - Obter dados do médico
- `GET /appointments/{appointmentId}` - Obter dados da consulta

#### hap-patients
- `GET /patients/{patientId}` - Obter dados do paciente

#### hap-auth
- `GET /api/auth/validate?token={token}` - Validar token

## Estrutura do Projeto

```
src/main/java/leti_sisdis_6/hapappointmentrecords/
├── api/                           # Controllers REST
│   └── AppointmentRecordController.java
├── config/                        # Configurações
│   └── HttpClientConfig.java
├── dto/                          # Data Transfer Objects
│   ├── input/                    # DTOs de entrada
│   ├── output/                   # DTOs de saída
│   └── external/                 # DTOs para comunicação externa
├── exceptions/                   # Exceções personalizadas
│   ├── MicroserviceCommunicationException.java
│   └── ...
├── http/                         # Cliente HTTP
│   └── ExternalServiceClient.java
├── model/                        # Entidades JPA
│   └── AppointmentRecord.java
├── repository/                   # Repositórios JPA
│   └── AppointmentRecordRepository.java
└── service/                      # Lógica de negócio
    └── AppointmentRecordService.java
```

## Configuração

### application.properties

```properties
# URLs dos serviços externos
hap.physicians.base-url=http://localhost:8081
hap.patients.base-url=http://localhost:8082
hap.auth.base-url=http://localhost:8084

# Configuração do servidor
server.port=8083
```

### Dependências Maven

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
```

## Funcionalidades

### 1. Criar Registo de Consulta
- **Endpoint**: `POST /api/appointment-records/{appointmentId}/record`
- **Autorização**: PHYSICIAN
- **Funcionalidade**: Cria um registo de consulta após verificar autorização via HTTP

### 2. Visualizar Registo
- **Endpoint**: `GET /api/appointment-records/{recordId}`
- **Autorização**: ADMIN, PATIENT
- **Funcionalidade**: Visualiza registo com dados do médico obtidos via HTTP

### 3. Listar Registos do Paciente
- **Endpoint**: `GET /api/appointment-records/patient/{patientId}`
- **Autorização**: PHYSICIAN
- **Funcionalidade**: Lista todos os registos de um paciente

## Tratamento de Erros

O módulo implementa tratamento robusto de erros de comunicação:

- **Retry**: Tentativas automáticas com backoff exponencial
- **Fallback**: Dados padrão quando serviços externos falham
- **Timeout**: Timeouts configurados para evitar bloqueios
- **Logging**: Logs detalhados para debugging

## Testes

Execute os testes de integração para verificar a comunicação:

```bash
mvn test -Dtest=AppointmentRecordIntegrationTest
```

## Execução

1. Certifique-se que os outros módulos estão em execução
2. Execute o módulo:
```bash
mvn spring-boot:run
```

3. Acesse a documentação Swagger em: `http://localhost:8083/swagger-ui.html`

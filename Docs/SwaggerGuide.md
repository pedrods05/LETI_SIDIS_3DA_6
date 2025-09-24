# PCM Clinic API

RESTful API for medical clinic management, developed with Spring Boot.

## API Documentation

The API documentation is available through Swagger UI and OpenAPI Specification.

### Swagger UI
The interactive Swagger UI interface allows you to view and test all API endpoints:
```
http://localhost:8080/swagger-ui.html
```

### OpenAPI Specification
The OpenAPI specification in JSON format can be accessed at:
```
http://localhost:8080/v3/api-docs
```

### Important Notes
- The application must be running to access the documentation
- The default server runs on port 8080
- Documentation is automatically generated from controller annotations
- All API endpoints are documented with request and response examples

## Requirements
- Java 17 or higher
- Maven
- Spring Boot 3.2.3
- SpringDoc OpenAPI 2.2.0
# Kong API Gateway for HAP

This folder contains the declarative configuration used to run Kong as the edge gateway for the HAP services.

## Services routed through Kong

| Service | Internal URL | External Route | Methods | Plugins |
|---------|--------------|----------------|---------|---------|
| doctors-service | http://hap-physicians:8080 | /api/v1/doctors | GET, POST | JWT, rate limiting, CORS, Prometheus |
| patients-service | http://hap-patients:8080 | /api/v1/patients | GET, POST | JWT, rate limiting, CORS, Prometheus |
| appointmentrecords-service | http://hap-appointmentrecords:8080 | /api/v1/appointments | GET, POST, PUT | JWT, rate limiting, CORS, Prometheus |
| auth-service | http://hap-auth:8080 | /api/v1/auth | POST | CORS, Prometheus |

## Running locally

```bash
cd C:/IdeaProjects/LETI_SIDIS_3DA_6
# start backing services + kong
powershell -Command "docker compose up kong-db kong-migrations kong -d"

# or full stack
powershell -Command "docker compose up -d"

# verify Kong admin API
curl http://localhost:8001/services

# exercise doctors route (requires valid JWT header)
curl -H "Authorization: Bearer <token>" http://localhost:8000/api/v1/doctors
```

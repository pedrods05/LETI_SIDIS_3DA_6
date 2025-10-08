FROM ubuntu:latest
LABEL authors="pedro"

ENTRYPOINT ["top", "-b"]
version: '3.8'

services:
  # API Gateway
  api-gateway:
    build:
      context: .
      dockerfile: api-gateway/Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JWT_SECRET=your-secret-key-change-in-production
    depends_on:
      - hap-doctors-a
      - hap-doctors-b
      - hap-clients-a
      - hap-clients-b
      - hap-consultation-services-a
      - hap-consultation-services-b
    networks:
      - hap-network

  # HAP Doctors - Instance A
  hap-doctors-a:
    build:
      context: .
      dockerfile: hap-doctors/Dockerfile
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JWT_SECRET=your-secret-key-change-in-production
      - PEER_LIST=http://hap-doctors-b:8082
    networks:
      - hap-network

  # HAP Doctors - Instance B
  hap-doctors-b:
    build:
      context: .
      dockerfile: hap-doctors/Dockerfile
    ports:
      - "8082:8082"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JWT_SECRET=your-secret-key-change-in-production
      - PEER_LIST=http://hap-doctors-a:8081
    networks:
      - hap-network

  # HAP Clients - Instance A
  hap-clients-a:
    build:
      context: .
      dockerfile: hap-clients/Dockerfile
    ports:
      - "8091:8091"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JWT_SECRET=your-secret-key-change-in-production
      - PEER_LIST=http://hap-clients-b:8092
    networks:
      - hap-network

  # HAP Clients - Instance B
  hap-clients-b:
    build:
      context: .
      dockerfile: hap-clients/Dockerfile
    ports:
      - "8092:8092"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JWT_SECRET=your-secret-key-change-in-production
      - PEER_LIST=http://hap-clients-a:8091
    networks:
      - hap-network

  # HAP Consultation Services - Instance A
  hap-consultation-services-a:
    build:
      context: .
      dockerfile: hap-consultation-services/Dockerfile
    ports:
      - "8071:8071"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JWT_SECRET=your-secret-key-change-in-production
      - PEER_LIST=http://hap-consultation-services-b:8072
    networks:
      - hap-network

  # HAP Consultation Services - Instance B
  hap-consultation-services-b:
    build:
      context: .
      dockerfile: hap-consultation-services/Dockerfile
    ports:
      - "8072:8072"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JWT_SECRET=your-secret-key-change-in-production
      - PEER_LIST=http://hap-consultation-services-a:8071
    networks:
      - hap-network

networks:
  hap-network:
    driver: bridge


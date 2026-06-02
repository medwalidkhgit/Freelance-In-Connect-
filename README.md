# FIC - Freelance-In-Connect-

## Overview

FIC (Freelance In Connect) is a B2B platform that connects companies with IT freelancers through a modern and scalable microservices architecture.

The platform enables companies to publish missions, manage applications, communicate with freelancers, and streamline recruitment and collaboration processes within a secure digital ecosystem.

Designed following cloud-native principles, FIC leverages modern technologies for security, scalability, maintainability, and distributed application development.

## Key Features

- Secure authentication and authorization
- Freelancer profile management
- Company onboarding and validation
- Mission publishing and management
- Application workflow management
- Real-time messaging
- Online payment integration
- Centralized API management
- Containerized deployment

## Technology Stack

### Backend
- JDK 17
- Spring Boot
- Spring Security
- Spring Data JPA
- OpenFeign

### Frontend
- React
- TypeScript

### Databases
- PostgreSQL
- MongoDB

### Security & API Management
- Keycloak
- WSO2 API Manager

### DevOps & Cloud
- Docker
- Docker Compose
- AWS (Deployment)

## Architecture

FIC is built using a microservices architecture where each service is responsible for a specific business domain. Authentication and identity management are handled by Keycloak, while WSO2 API Manager acts as the API Gateway and single entry point for all client requests.

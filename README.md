# E-Commerce Microservices Platform

[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.oracle.com/java/technologies/downloads/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.5-red.svg)](https://kafka.apache.org/)
[![AWS](https://img.shields.io/badge/AWS-Cloud%20Native-orange.svg)](https://aws.amazon.com/)

> Event-driven e-commerce platform demonstrating enterprise microservices architecture with Spring Boot, Apache Kafka, and AWS deployment.

## 📊 Project Overview

A complete e-commerce system built with modern microservices architecture:

- **6 Independent Microservices** (User, Product, Order, Payment, Notification, API Gateway)
- **Apache Kafka** for event-driven communication
- **Spring Boot 3.1.5** with best practices
- **PostgreSQL** databases (one per service)
- **AWS Deployment** (ECS, RDS, MSK, S3, CloudFront)
- **GitHub Actions** CI/CD pipeline
- **Angular 17** frontend

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- Git 2.30+

### Local Setup

```bash
# Clone repository
git clone https://github.com/YOUR_USERNAME/ecommerce-microservices-kafka-aws.git
cd ecommerce-microservices-kafka-aws

# Start infrastructure
docker-compose up -d

# Build services
mvn clean install -DskipTests -f ecommerce-parent/pom.xml

# Run services (each in separate terminal)
# User Service
cd user-service && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"

# Product Service
cd product-service && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"

# Order Service
cd order-service && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"

# Payment Service
cd payment-service && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8084"

# Notification Service
cd notification-service && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8085"

# API Gateway
cd api-gateway && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
```

### Access Services
- API Gateway: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Kafka UI: http://localhost:8161
- pgAdmin: http://localhost:5050

## 📁 Project Structure

```
ecommerce-microservices-kafka-aws/
├── ecommerce-parent/              # Parent POM (dependency management)
├── user-service/                  # User & Authentication Service
├── product-service/               # Product Catalog Service
├── order-service/                 # Order Management Service
├── payment-service/               # Payment Processing Service
├── notification-service/          # Notification Service
├── api-gateway/                   # API Gateway
├── docs/                          # Documentation
│   ├── ARCHITECTURE.md
│   ├── DATABASE_SCHEMA.md
│   └── PROJECT_STRUCTURE.md
├── docker-compose.yml
└── README.md
```

## 📚 Documentation

- [Architecture Design](docs/ARCHITECTURE.md) - System design and patterns
- [Database Schema](docs/DATABASE_SCHEMA.md) - SQL schemas for all services
- [Project Structure](docs/PROJECT_STRUCTURE.md) - Folder layout and organization

## 🎓 Learning Outcomes

✅ Microservices architecture  
✅ Spring Boot best practices  
✅ Apache Kafka event streaming  
✅ REST API design  
✅ Docker containerization  
✅ AWS cloud deployment  
✅ CI/CD automation  
✅ Distributed system patterns

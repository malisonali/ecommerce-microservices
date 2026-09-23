# Project Structure

## Directory Layout

```
ecommerce-microservices/
│
├── ecommerce-parent/                          # Parent Maven module
│   ├── pom.xml                                # Parent POM (dependency management)
│   └── README.md
│
├── user-service/                              # Microservice 1
│   ├── pom.xml                                # Service-specific dependencies
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/ecommerce/user/
│   │   │   │   ├── UserServiceApplication.java
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   └── UserController.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   └── UserService.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   └── RoleRepository.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── User.java
│   │   │   │   │   └── Role.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── LoginResponse.java
│   │   │   │   │   └── UserDTO.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   └── JwtTokenProvider.java
│   │   │   │   ├── exception/
│   │   │   │   │   ├── UserNotFoundException.java
│   │   │   │   │   ├── InvalidCredentialsException.java
│   │   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │   └── filter/
│   │   │   │       └── JwtAuthenticationFilter.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-local.yml
│   │   │       ├── application-dev.yml
│   │   │       └── application-prod.yml
│   │   └── test/
│   │       └── java/com/ecommerce/user/
│   │           ├── UserServiceApplicationTests.java
│   │           ├── UserServiceTest.java
│   │           ├── AuthControllerTest.java
│   │           └── UserRepositoryTest.java
│   └── Dockerfile
│
├── product-service/                           # Microservice 2
│   ├── pom.xml
│   └── src/ (similar structure to user-service)
│       ├── main/java/com/ecommerce/product/
│       │   ├── ProductServiceApplication.java
│       │   ├── controller/
│       │   ├── service/
│       │   ├── repository/
│       │   ├── entity/
│       │   ├── dto/
│       │   ├── config/
│       │   ├── exception/
│       │   ├── specification/
│       │   └── resources/
│       └── test/
│
├── order-service/                             # Microservice 3
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/ecommerce/order/
│       │   ├── OrderServiceApplication.java
│       │   ├── controller/
│       │   ├── service/
│       │   ├── repository/
│       │   ├── entity/
│       │   ├── dto/
│       │   ├── config/
│       │   ├── client/                       # Feign clients
│       │   │   ├── ProductServiceClient.java
│       │   │   └── UserServiceClient.java
│       │   ├── event/
│       │   │   └── OrderCreatedEvent.java
│       │   ├── publisher/
│       │   │   └── OrderEventPublisher.java
│       │   ├── exception/
│       │   ├── resources/
│       │   └── resources/
│       └── test/
│
├── payment-service/                           # Microservice 4
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/ecommerce/payment/
│       │   ├── PaymentServiceApplication.java
│       │   ├── controller/
│       │   ├── service/
│       │   ├── repository/
│       │   ├── entity/
│       │   ├── dto/
│       │   ├── config/
│       │   ├── gateway/
│       │   │   └── PaymentGateway.java
│       │   ├── event/
│       │   ├── publisher/
│       │   ├── exception/
│       │   └── resources/
│       └── test/
│
├── notification-service/                      # Microservice 5
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/ecommerce/notification/
│       │   ├── NotificationServiceApplication.java
│       │   ├── controller/                    # Few endpoints (mainly event-driven)
│       │   ├── service/
│       │   ├── repository/
│       │   ├── entity/
│       │   ├── consumer/
│       │   │   ├── OrderEventConsumer.java
│       │   │   └── PaymentEventConsumer.java
│       │   ├── publisher/
│       │   │   ├── EmailPublisher.java
│       │   │   └── SmsPublisher.java
│       │   ├── config/
│       │   ├── exception/
│       │   ├── event/
│       │   └── resources/
│       └── test/
│
├── api-gateway/                               # Microservice 6
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/ecommerce/gateway/
│       │   ├── ApiGatewayApplication.java
│       │   ├── config/
│       │   │   ├── GatewayConfig.java        # Spring Cloud Gateway routes
│       │   │   ├── SecurityConfig.java
│       │   │   └── CorsConfig.java
│       │   ├── filter/
│       │   │   ├── AuthenticationFilter.java # JWT validation
│       │   │   ├── LoggingFilter.java
│       │   │   └── RateLimitingFilter.java
│       │   ├── exception/
│       │   └── resources/
│       │       └── application.yml
│       └── test/
│
├── docs/                                      # Documentation
│   ├── ARCHITECTURE.md                        # System architecture overview
│   ├── DATABASE_SCHEMA.md                     # SQL schemas
│   ├── PROJECT_STRUCTURE.md                   # This file
│   ├── API_DOCUMENTATION.md                   # API endpoints reference
│   ├── DEVELOPMENT_GUIDE.md                   # How to develop new features
│   ├── AWS_DEPLOYMENT.md                      # AWS deployment steps
│   ├── TROUBLESHOOTING.md                     # Common issues & solutions
│   └── SEQUENCE_DIAGRAMS.md                   # API flow diagrams
│
├── angular-frontend/                          # Angular Frontend (if included)
│   ├── package.json
│   ├── angular.json
│   ├── tsconfig.json
│   ├── src/
│   │   ├── app/
│   │   │   ├── app.component.ts
│   │   │   ├── core/
│   │   │   │   ├── services/
│   │   │   │   │   ├── auth.service.ts
│   │   │   │   │   ├── product.service.ts
│   │   │   │   │   ├── order.service.ts
│   │   │   │   │   └── payment.service.ts
│   │   │   │   ├── interceptors/
│   │   │   │   │   └── auth.interceptor.ts
│   │   │   │   └── guards/
│   │   │   │       └── auth.guard.ts
│   │   │   ├── modules/
│   │   │   │   ├── auth/
│   │   │   │   │   ├── login/
│   │   │   │   │   ├── register/
│   │   │   │   │   └── auth.module.ts
│   │   │   │   ├── products/
│   │   │   │   │   ├── product-list/
│   │   │   │   │   ├── product-detail/
│   │   │   │   │   └── products.module.ts
│   │   │   │   ├── orders/
│   │   │   │   │   ├── order-list/
│   │   │   │   │   ├── checkout/
│   │   │   │   │   └── orders.module.ts
│   │   │   │   └── cart/
│   │   │   │       ├── cart.component.ts
│   │   │   │       └── cart.module.ts
│   │   │   └── shared/
│   │   │       ├── components/
│   │   │       ├── models/
│   │   │       └── shared.module.ts
│   │   └── environments/
│   │       ├── environment.ts
│   │       └── environment.prod.ts
│   └── Dockerfile
│
├── .github/                                   # GitHub configuration
│   ├── workflows/
│   │   ├── ci-pipeline.yml                   # Run tests on push
│   │   └── deploy.yml                        # Deploy to AWS on release
│   └── CODEOWNERS
│
├── docker-compose.yml                         # Local development setup
├── Dockerfile                                 # Multi-stage build for services
├── .gitignore                                 # Git ignore rules
├── .env.example                               # Environment variables template
├── README.md                                  # Main documentation
├── pom.xml                                    # Root POM (optional)
└── LICENSE                                    # MIT License
```

## Folder Descriptions

### ecommerce-parent/
**Purpose:** Parent Maven module managing dependencies for all services

**Key File:** `pom.xml`
- Defines Spring Boot version (3.1.5)
- Manages dependencies
- All services inherit from this

### Each Microservice Folder (user-service, product-service, etc.)

#### src/main/java/com/ecommerce/{service}/

**controller/**
- REST endpoints
- Request/response handling
- HTTP status codes
- Example: `AuthController` with `/auth/login` endpoint

**service/**
- Business logic
- Core functionality
- Orchestration
- Example: `AuthService` with login logic

**repository/**
- Spring Data JPA repositories
- Database queries
- CRUD operations
- Example: `UserRepository.findByEmail()`

**entity/**
- JPA entities
- Database models
- Annotations (@Entity, @Table, @Column)
- Example: `User` entity mapped to `users` table

**dto/**
- Data Transfer Objects
- API request/response models
- Validation annotations
- Example: `LoginRequest` with email and password

**config/**
- Spring configuration
- Security config
- Kafka config
- Example: `SecurityConfig` for JWT

**exception/**
- Custom exception classes
- Global exception handler
- Error responses
- Example: `UserNotFoundException`

**client/** (for services calling other services)
- Feign clients for REST calls
- Example: `ProductServiceClient` in order-service

**consumer/** (for Kafka consumers)
- Kafka consumer implementation
- Event listeners
- Example: `OrderEventConsumer` in notification-service

**publisher/** (for Kafka producers)
- Kafka event publishers
- Example: `OrderEventPublisher` in order-service

**event/**
- Event models/DTOs
- Example: `OrderCreatedEvent`

**filter/**
- Web filters
- Example: `JwtAuthenticationFilter`

#### src/main/resources/

**application.yml**
- Spring Boot configuration
- Database connection
- Server port
- Logging level

**application-{profile}.yml**
- Environment-specific configs
- Example: `application-prod.yml` for production

#### src/test/

**Test Files**
- Unit tests (service layer)
- Integration tests (controller layer)
- Repository tests
- Example: `UserServiceTest`

### docs/

**ARCHITECTURE.md** - System design and patterns  
**DATABASE_SCHEMA.md** - SQL schemas  
**PROJECT_STRUCTURE.md** - This file  
**API_DOCUMENTATION.md** - REST API reference  
**DEVELOPMENT_GUIDE.md** - How to add features  
**AWS_DEPLOYMENT.md** - Production deployment  
**TROUBLESHOOTING.md** - Common issues  

### docker-compose.yml

Defines local development infrastructure:
- PostgreSQL databases (5 instances)
- Apache Kafka
- Zookeeper (for Kafka)
- Kafka UI (management)
- pgAdmin (database management)
- Redis (caching)

---

## Key Patterns

### Layer Architecture (Each Service)

```
Controller (REST API)
    ↓
Service (Business Logic)
    ↓
Repository (Database Access)
    ↓
Entity (JPA Model)
    ↓
Database (PostgreSQL)
```

### DTO Pattern

```
API Request
    ↓
Request DTO (validation)
    ↓
Service Logic
    ↓
Response DTO (serialization)
    ↓
API Response
```

### Exception Handling

```
controller/ -> catch exceptions
    ↓
GlobalExceptionHandler
    ↓
HTTP Error Response (400, 401, 404, 500)
```

---

## File Naming Conventions

### Classes
```
UserService.java          (noun + verb as suffix)
AuthController.java       (noun + Controller suffix)
UserRepository.java       (noun + Repository suffix)
UserNotFoundException.java (noun + Exception suffix)
```

### Files
```
application.yml           (lowercase with hyphens)
JwtTokenProvider.java     (PascalCase)
```

### Packages
```
com.ecommerce.user        (lowercase)
com.ecommerce.user.controller
com.ecommerce.user.service
com.ecommerce.user.repository
```

---

## Maven Multi-Module Structure

```
ecommerce-parent/ (root)
├── pom.xml (parent POM)
│   └── <modules>
│       ├── user-service
│       ├── product-service
│       ├── order-service
│       ├── payment-service
│       ├── notification-service
│       └── api-gateway

Building:
mvn clean install -f ecommerce-parent/pom.xml
(builds all modules)

Building single service:
mvn clean install -pl user-service
```

---

## Project Growth Timeline

### Day 1
```
ecommerce-microservices/
├── docs/
├── .gitignore
├── README.md
└── (empty service folders)
```

### Days 2-7
```
Add to each service:
├── pom.xml
└── src/main/java/com/ecommerce/{service}/
    ├── entity/
    ├── repository/
    ├── service/
    ├── controller/
    └── config/
```

### Days 8-14
```
Add:
├── Kafka integration
├── Client/ (Feign clients)
├── Consumer/ (Kafka consumers)
└── Event/ (Event models)
```

### Days 15-21
```
Add:
├── angular-frontend/
└── docker-compose.yml
```

### Days 22-30
```
Add:
├── .github/workflows/
├── Dockerfile
└── AWS configuration
```

---

## IDE Setup (IntelliJ IDEA)

### Opening the Project

1. Open IntelliJ IDEA
2. Click **File → Open**
3. Select the `ecommerce-parent` folder
4. Click **Open as Project**
5. IDE will automatically recognize Maven multi-module project

### Running Microservices

**Method 1: Using Run Configuration (Recommended)**

1. Go to **Run → Edit Configurations**
2. Click **+** to add new configuration
3. Select **Spring Boot**
4. Fill in:
   - Name: `User Service`
   - Main class: `com.ecommerce.user.UserServiceApplication`
   - Working directory: `user-service`
   - VM options: `-Dserver.port=8081`
5. Click **OK**
6. Click the green **Run** button or press **Shift + F10**

**Method 2: Quick Run (Faster)**

1. Open any service's main class (e.g., `UserServiceApplication.java`)
2. Look for green **▶** arrow next to class name
3. Click it → Select **Run**
4. Service starts automatically

### Running All Services

Open 6 terminal tabs (one per service):

```bash
# Terminal 1: User Service
cd user-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"

# Terminal 2: Product Service
cd product-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"

# Terminal 3: Order Service
cd order-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"

# Terminal 4: Payment Service
cd payment-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8084"

# Terminal 5: Notification Service
cd notification-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8085"

# Terminal 6: API Gateway
cd api-gateway
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
```

### Useful IntelliJ Shortcuts

| Shortcut | Action |
|----------|--------|
| **Ctrl + Alt + Shift + S** | Project Structure |
| **Ctrl + Shift + O** | Optimize Imports |
| **Ctrl + Alt + L** | Reformat Code |
| **Shift + F10** | Run |
| **Shift + F9** | Debug |
| **Ctrl + Shift + F10** | Run in context |
| **Alt + 6** | View Problems |
| **Ctrl + P** | Parameter Info |
| **Ctrl + Q** | Quick Documentation |

### IntelliJ Maven Configuration

**Enable Maven Auto-Reload:**
1. File → Settings → Build, Execution, Deployment → Build Tools → Maven
2. Enable **Always update snapshots**
3. Check **Show Projects roots in External Libraries**

**Run Maven Build:**
1. View → Tool Windows → Maven (or Alt + 1)
2. Double-click the goal you want to run
3. Or: Run → Maven → Clean, Compile, etc.

### IntelliJ Terminal

- Press **Alt + F12** to open terminal in IntelliJ
- Run Maven commands directly from IDE terminal
- See output in real-time

---

See [ARCHITECTURE.md](ARCHITECTURE.md) for system design.

# System Architecture

## Overview

E-Commerce Microservices Platform is built with a distributed, event-driven architecture.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Applications                      │
│              (Web, Mobile, Desktop, APIs)                   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│     AWS CloudFront + S3 (Global Content Delivery)          │
│              Angular Frontend Application                   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│        AWS Application Load Balancer (ALB)                 │
│    • Route traffic to API Gateway                          │
│    • SSL/TLS termination                                   │
│    • Health checks                                         │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│         Spring Cloud API Gateway (Port 8080)              │
│    • Single entry point for all services                  │
│    • JWT authentication & authorization                   │
│    • Request/response filtering                           │
│    • Rate limiting                                        │
└─┬──────────┬───────────┬────────────┬──────────┬──────────┘
  │          │           │            │          │
  ↓          ↓           ↓            ↓          ↓
┌────────────────────────────────────────────────────────────┐
│              Microservices (AWS ECS Fargate)              │
│                                                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │ User Service │  │Product Srv   │  │Order Service │   │
│  │  :8081       │  │  :8082       │  │  :8083       │   │
│  └──────────────┘  └──────────────┘  └──────────────┘   │
│                                                            │
│  ┌──────────────┐  ┌──────────────┐                      │
│  │Payment Srv   │  │Notification  │                      │
│  │  :8084       │  │Srv :8085     │                      │
│  └──────────────┘  └──────────────┘                      │
│                                                            │
└───────────────────────┬──────────────────────────────────┘
                        │
          ┌─────────────┼─────────────┐
          │             │             │
          ↓             ↓             ↓
┌───────────────────────────────────────────────────────────┐
│           AWS MSK (Managed Kafka Cluster)                │
│                                                           │
│  Topics:                                                 │
│  • orders.created                                        │
│  • payments.processed                                    │
│  • user.registered                                       │
│                                                           │
└───────────────────────────────────────────────────────────┘
          │             │             │
          ↓             ↓             ↓
┌───────────────────────────────────────────────────────────┐
│   AWS RDS PostgreSQL + AWS ElastiCache Redis             │
│                                                           │
│  Databases:              Caching:                        │
│  • user_db              • Product catalog                │
│  • product_db           • User sessions                  │
│  • order_db             • Frequently accessed data       │
│  • payment_db                                            │
│  • notification_db                                       │
│                                                           │
└───────────────────────────────────────────────────────────┘
          │
          ↓
┌───────────────────────────────────────────────────────────┐
│     AWS CloudWatch + X-Ray (Observability)               │
│  • Centralized logging                                   │
│  • Distributed tracing                                   │
│  • Metrics & alarms                                      │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

## Microservices Breakdown

### User Service (Port 8081)

**Responsibility:** User management and authentication

**Owns:**
- User accounts
- Authentication (login, register)
- User roles and permissions
- JWT token management

**Endpoints:**
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh-token
GET    /api/v1/users/{id}
PUT    /api/v1/users/{id}
```

**Database:**
```
user_db:
- users (id, email, username, password_hash, created_at)
- roles (id, name)
- user_roles (user_id, role_id)
```

### Product Service (Port 8082)

**Responsibility:** Product catalog management

**Owns:**
- Product inventory
- Categories
- Product reviews and ratings
- Search functionality

**Endpoints:**
```
GET    /api/v1/products
GET    /api/v1/products/{id}
POST   /api/v1/products
PUT    /api/v1/products/{id}
DELETE /api/v1/products/{id}
```

**Database:**
```
product_db:
- products (id, name, price, stock_quantity, category_id)
- categories (id, name)
- product_reviews (id, product_id, user_id, rating, comment)
```

### Order Service (Port 8083)

**Responsibility:** Order management and shopping cart

**Owns:**
- Order creation and tracking
- Shopping cart management
- Order status transitions

**Endpoints:**
```
POST   /api/v1/orders
GET    /api/v1/orders/{id}
GET    /api/v1/orders
PUT    /api/v1/orders/{id}/cancel
POST   /api/v1/cart/items
```

**Database:**
```
order_db:
- orders (id, user_id, total_amount, status, created_at)
- order_items (id, order_id, product_id, quantity, price)
- carts (id, user_id)
- cart_items (id, cart_id, product_id, quantity)
```

**Publishes Events:**
- `orders.created` → When order is created
- `orders.confirmed` → When payment confirmed

### Payment Service (Port 8084)

**Responsibility:** Payment processing

**Owns:**
- Payment processing
- Transaction management
- Refunds
- Payment history

**Endpoints:**
```
POST   /api/v1/payments
GET    /api/v1/payments/{id}
POST   /api/v1/payments/{id}/refund
```

**Database:**
```
payment_db:
- payments (id, order_id, amount, status, transaction_id)
- payment_methods (id, user_id, card_token)
- payment_status_history (id, payment_id, from_status, to_status)
```

**Publishes Events:**
- `payments.processed` → When payment succeeds
- `payments.failed` → When payment fails

### Notification Service (Port 8085)

**Responsibility:** Sending notifications

**Owns:**
- Email notifications
- SMS notifications
- Notification history

**Consumes Events (No REST API):**
- `orders.created` → Send order confirmation email
- `orders.shipped` → Send shipping notification
- `payments.processed` → Send payment receipt

**Database:**
```
notification_db:
- notifications (id, user_id, type, content, is_read)
- email_logs (id, recipient, subject, status, sent_at)
```

### API Gateway

**Responsibility:** Request routing and authentication

**Features:**
- Routes requests to appropriate services
- Validates JWT tokens
- Rate limiting
- Request/response filtering
- CORS handling

**Routes:**
```
/api/v1/auth/** → User Service (8081)
/api/v1/users/** → User Service (8081)
/api/v1/products/** → Product Service (8082)
/api/v1/orders/** → Order Service (8083)
/api/v1/payments/** → Payment Service (8084)
/api/v1/cart/** → Order Service (8083)
```

## Communication Patterns

### Synchronous (REST) - When immediate response needed

```
Order Service (needs to check inventory)
    ↓
REST Call to Product Service
    ↓
Product Service responds immediately
    ↓
Order Service validates and proceeds
```

**When to use:**
- Real-time data validation
- Need immediate response
- Short-lived operations

### Asynchronous (Kafka) - When response not immediately needed

```
Order Service (publishes event)
    ↓
OrderCreated Event → Kafka Topic
    ↓
Notification Service consumes (asynchronously)
    ↓
Sends email in background
```

**When to use:**
- Non-blocking operations
- Don't care about response timing
- Multiple consumers interested in event

## Database Strategy

### Database per Service Pattern

```
User Service      → user_db
Product Service   → product_db
Order Service     → order_db
Payment Service   → payment_db
Notification Svc  → notification_db
```

**Benefits:**
✅ Independent scaling  
✅ Schema flexibility  
✅ Technology choice per service  
✅ Clear data ownership  
✅ Disaster recovery isolation  

**Challenges:**
⚠️ Distributed transactions  
⚠️ Data consistency  
⚠️ Joins across databases  

**Solutions:**
- Use eventual consistency via Kafka events
- Kafka acts as event log
- Services subscribe to events they care about

## API Versioning

All endpoints use versioning:

```
/api/v1/...     Current version
/api/v2/...     Future version (backward compatibility)
```

## Authentication & Security

### JWT Token Flow

```
1. User logs in
   POST /api/v1/auth/login
   Response: { token, refreshToken, expiresIn }

2. Client stores token in localStorage

3. Client sends token with requests
   Authorization: Bearer <token>

4. API Gateway validates token

5. API Gateway forwards to service with user info

6. Service authorizes and returns data
```

### Token Refresh

```
Old token expires
   ↓
Client sends refresh token
   ↓
User Service creates new token
   ↓
Client uses new token
```

## Deployment Architecture (AWS)

### Production Environment

```
DNS (Route53)
    ↓
CloudFront (CDN for static content)
    ↓
ALB (Application Load Balancer)
    ↓
ECS Fargate (Containerized services)
    ↓
RDS (Managed PostgreSQL)
    ↓
MSK (Managed Kafka)
    ↓
ElastiCache (Redis for caching)
    ↓
CloudWatch (Monitoring & Logging)
    ↓
X-Ray (Distributed Tracing)
```

## Scaling Strategy

### Horizontal Scaling

```
User Service (2 instances)
Product Service (3 instances)
Order Service (2 instances)
Payment Service (1 instance)
Notification Service (1 instance)
```

Each service can scale independently based on load.

### Caching Strategy

```
Redis Cache:
- Product catalog (frequently accessed)
- User sessions
- Category listings
- Recent orders

Cache invalidation:
- TTL-based (Time To Live)
- Event-based (Kafka triggers cache update)
- Manual invalidation (admin action)
```

## Monitoring & Observability

### CloudWatch Logs

All services send logs to CloudWatch:
```
- Application logs
- Request logs
- Error logs
- Business metrics
```

### X-Ray Tracing

Distributed tracing for request flow:
```
User Request
    ↓ (traced)
API Gateway
    ↓ (traced)
Order Service
    ↓ (traced)
Product Service (Feign call)
    ↓ (traced)
Kafka
    ↓ (traced)
Notification Service (consumer)
```

### Metrics

- CPU utilization
- Memory usage
- Request latency
- Error rates
- Business metrics (orders/min, payments/min)

## Future Considerations

- **Service Mesh** (Istio) - For advanced traffic management
- **Event Sourcing** - For audit trail
- **CQRS** - Command Query Responsibility Segregation
- **GraphQL** - Alternative to REST
- **Kubernetes** - Alternative to ECS

---

See [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) for SQL schemas.

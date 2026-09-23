# Database Schema

## Overview

Each microservice has its own PostgreSQL database following the "Database per Service" pattern.

## Database Instances

```
5 PostgreSQL Databases (one per service)
├── user_db              (User Service)
├── product_db           (Product Service)
├── order_db             (Order Service)
├── payment_db           (Payment Service)
└── notification_db      (Notification Service)
```

---

## 1. USER_DB (User Service)

### users Table

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT email_format CHECK (email ~ '^[^@]+@[^@]+\.[^@]+$')
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_active ON users(is_active);
```

### roles Table

```sql
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_role_name CHECK (name IN ('ADMIN', 'USER', 'SELLER'))
);

-- Insert default roles
INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Administrator with full access'),
    ('USER', 'Regular user'),
    ('SELLER', 'Seller with product management access');
```

### user_roles Table (Many-to-Many)

```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
```

---

## 2. PRODUCT_DB (Product Service)

### categories Table

```sql
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_categories_active ON categories(is_active);
CREATE INDEX idx_categories_name ON categories(name);
```

### products Table

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT positive_price CHECK (price > 0),
    CONSTRAINT non_negative_stock CHECK (stock_quantity >= 0)
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_active ON products(is_active);
CREATE INDEX idx_products_name ON products(name);
```

### product_reviews Table

```sql
CREATE TABLE product_reviews (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reviews_product ON product_reviews(product_id);
CREATE INDEX idx_reviews_user ON product_reviews(user_id);
CREATE INDEX idx_reviews_rating ON product_reviews(rating);
```

---

## 3. ORDER_DB (Order Service)

### carts Table

```sql
CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_carts_user ON carts(user_id);
```

### cart_items Table

```sql
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    price DECIMAL(10, 2) NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_cart_product UNIQUE(cart_id, product_id)
);

CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);
CREATE INDEX idx_cart_items_product ON cart_items(product_id);
```

### orders Table

```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10, 2) NOT NULL,
    shipping_address TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT positive_amount CHECK (total_amount > 0)
);

CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created ON orders(created_at);
```

### order_items Table

```sql
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    price DECIMAL(10, 2) NOT NULL,
    
    CONSTRAINT unique_order_product UNIQUE(order_id, product_id)
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);
```

---

## 4. PAYMENT_DB (Payment Service)

### payment_methods Table

```sql
CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    card_token VARCHAR(255) NOT NULL,
    last_four_digits VARCHAR(4),
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_type CHECK (type IN ('CREDIT_CARD', 'DEBIT_CARD', 'PAYPAL', 'BANK_TRANSFER'))
);

CREATE INDEX idx_payment_methods_user ON payment_methods(user_id);
CREATE INDEX idx_payment_methods_default ON payment_methods(user_id, is_default);
```

### payments Table

```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(255),
    payment_method_id BIGINT REFERENCES payment_methods(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    CONSTRAINT positive_amount CHECK (amount > 0)
);

CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payments_user ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_transaction ON payments(transaction_id);
```

### payment_status_history Table

```sql
CREATE TABLE payment_status_history (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(255),
    reason TEXT
);

CREATE INDEX idx_status_history_payment ON payment_status_history(payment_id);
CREATE INDEX idx_status_history_changed ON payment_status_history(changed_at);
```

---

## 5. NOTIFICATION_DB (Notification Service)

### notifications Table

```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_type CHECK (type IN ('ORDER', 'PAYMENT', 'SHIPPING', 'PROMOTION', 'SYSTEM'))
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_read ON notifications(is_read);
CREATE INDEX idx_notifications_created ON notifications(created_at);
```

### email_logs Table

```sql
CREATE TABLE email_logs (
    id BIGSERIAL PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    content TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    error_message TEXT,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'BOUNCED'))
);

CREATE INDEX idx_email_logs_status ON email_logs(status);
CREATE INDEX idx_email_logs_recipient ON email_logs(recipient_email);
CREATE INDEX idx_email_logs_created ON email_logs(created_at);
```

---

## Connection Details

### Local Development (Docker Compose)

```yaml
Services:
  user_db:        postgres:15 on localhost:5432 (database: user_db)
  product_db:     postgres:15 on localhost:5433 (database: product_db)
  order_db:       postgres:15 on localhost:5434 (database: order_db)
  payment_db:     postgres:15 on localhost:5435 (database: payment_db)
  notification_db: postgres:15 on localhost:5436 (database: notification_db)

Default credentials:
  Username: postgres
  Password: postgres
```

### AWS Production (RDS)

```
Each database runs in separate RDS instance:
- Multi-AZ for high availability
- Automated backups
- Read replicas for scaling
- CloudWatch monitoring
- Encryption at rest and in transit
```

### Accessing Databases

**Local pgAdmin:**
```
URL: http://localhost:5050
Username: admin@example.com
Password: admin
```

**AWS RDS:**
```
AWS Console → RDS → Databases
View: Endpoints, security groups, backups
```

---

## Design Principles

### 1. **Database per Service**
Each service owns its data. No direct database access between services.

### 2. **Data Isolation**
Services share data through APIs (REST) or events (Kafka), not database queries.

### 3. **Referential Integrity**
- Within a service database: Use foreign keys
- Across services: Use explicit IDs (no foreign keys to other databases)

### 4. **Indexing Strategy**
```
Index on:
- Primary keys (automatic)
- Foreign keys
- Frequently filtered columns (status, created_at)
- Frequently sorted columns (created_at, name)
- High-cardinality columns (email, username)

Avoid indexing:
- Low-cardinality columns (boolean flags)
- Very large columns (TEXT without LIMIT)
- Rarely queried columns
```

### 5. **Constraints**
```
✅ CHECK constraints for valid values
✅ UNIQUE constraints for business logic
✅ NOT NULL for required fields
✅ DEFAULT values for new records
```

---

## Migration Strategy

Use Flyway or Liquibase for schema versioning:

```
V1_0__initial_schema.sql      (users, roles, user_roles)
V1_1__add_indexes.sql         (all indexes)
V1_2__add_audit_tables.sql    (audit logging)
V2_0__add_new_fields.sql      (new columns)
```

---

## Backup & Recovery

### Automated Backups
- Daily automated backups
- 30-day retention (configurable)
- Point-in-time recovery available

### Manual Backups
```bash
# Export database
pg_dump -h localhost -U postgres -d user_db > user_db_backup.sql

# Restore database
psql -h localhost -U postgres -d user_db < user_db_backup.sql
```

---

See [ARCHITECTURE.md](ARCHITECTURE.md) for system design overview.

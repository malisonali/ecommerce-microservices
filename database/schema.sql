-- =====================================================================
-- E-commerce Microservices - PostgreSQL Schema
--
-- One database per service. Run each section against its own database:
--   user_db         -> localhost:5432
--   product_db      -> localhost:5433
--   order_db        -> localhost:5434
--   payment_db      -> localhost:5435
--   notification_db -> localhost:5436
--
-- Cross-service references (e.g. user_id in order_db) are plain BIGINT
-- columns with no foreign key, since the referenced table lives in a
-- different database.
--
-- Every section is idempotent and safe to re-run.
-- =====================================================================


-- ============ USER_DB ============

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_roles_name    UNIQUE (name),
    CONSTRAINT chk_roles_name   CHECK (name IN ('ADMIN', 'USER', 'SELLER'))
);

CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100),
    last_name     VARCHAR(100),
    phone         VARCHAR(20),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_users_email_format
        CHECK (email ~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$'),
    CONSTRAINT chk_users_username_length
        CHECK (char_length(username) >= 3)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id     BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id     BIGINT    NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, role_id)
);

-- Case-insensitive uniqueness: "Alice@x.com" and "alice@x.com" are the same account.
-- These unique indexes also serve as the lookup indexes on email and username.
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email    ON users (LOWER(email));
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_username ON users (LOWER(username));
CREATE INDEX IF NOT EXISTS idx_users_is_active      ON users (is_active);
-- user_id is covered by the primary key; role_id needs its own index.
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id   ON user_roles (role_id);

CREATE OR REPLACE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

INSERT INTO roles (name, description) VALUES
    ('ADMIN',  'Administrator with full system access'),
    ('USER',   'Regular customer'),
    ('SELLER', 'Seller who can manage their own products')
ON CONFLICT (name) DO NOTHING;


-- ============ PRODUCT_DB ============

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS products (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255)   NOT NULL,
    description    TEXT,
    price          NUMERIC(10, 2) NOT NULL,
    stock_quantity INTEGER        NOT NULL DEFAULT 0,
    category_id    BIGINT         NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    image_url      VARCHAR(500),
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_products_price_positive     CHECK (price > 0),
    CONSTRAINT chk_products_stock_non_negative CHECK (stock_quantity >= 0)
);

CREATE TABLE IF NOT EXISTS product_reviews (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT    NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id    BIGINT    NOT NULL,  -- references user_db.users(id)
    rating     SMALLINT  NOT NULL,
    comment    TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_reviews_rating_range  CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT uk_reviews_product_user   UNIQUE (product_id, user_id)  -- one review per user per product
);

-- category name is covered by uk_categories_name.
CREATE INDEX IF NOT EXISTS idx_categories_is_active ON categories (is_active);
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products (category_id);
CREATE INDEX IF NOT EXISTS idx_products_is_active   ON products (is_active);
CREATE INDEX IF NOT EXISTS idx_products_name        ON products (name);
-- product_id is covered by uk_reviews_product_user (leading column).
CREATE INDEX IF NOT EXISTS idx_reviews_user_id      ON product_reviews (user_id);

CREATE OR REPLACE TRIGGER trg_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE TRIGGER trg_product_reviews_updated_at
    BEFORE UPDATE ON product_reviews
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============ ORDER_DB ============

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS carts (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,  -- references user_db.users(id)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_carts_user UNIQUE (user_id)  -- one active cart per user
);

CREATE TABLE IF NOT EXISTS cart_items (
    id         BIGSERIAL PRIMARY KEY,
    cart_id    BIGINT         NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id BIGINT         NOT NULL,  -- references product_db.products(id)
    quantity   INTEGER        NOT NULL,
    price      NUMERIC(10, 2) NOT NULL,  -- unit price snapshot when added
    added_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_cart_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_cart_items_price_positive    CHECK (price > 0),
    CONSTRAINT uk_cart_items_cart_product       UNIQUE (cart_id, product_id)
);

CREATE TABLE IF NOT EXISTS orders (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT         NOT NULL,  -- references user_db.users(id)
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    total_amount     NUMERIC(12, 2) NOT NULL,
    shipping_address TEXT           NOT NULL,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_orders_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT chk_orders_total_positive CHECK (total_amount > 0)
);

CREATE TABLE IF NOT EXISTS order_items (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT         NOT NULL,  -- references product_db.products(id)
    quantity   INTEGER        NOT NULL,
    price      NUMERIC(10, 2) NOT NULL,  -- unit price at time of purchase

    CONSTRAINT chk_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price_positive    CHECK (price > 0),
    CONSTRAINT uk_order_items_order_product      UNIQUE (order_id, product_id)
);

-- carts.user_id is covered by uk_carts_user; cart_items.cart_id and
-- order_items.order_id are covered by their composite unique constraints.
CREATE INDEX IF NOT EXISTS idx_cart_items_product_id  ON cart_items (product_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_id         ON orders (user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status          ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at      ON orders (created_at);
CREATE INDEX IF NOT EXISTS idx_orders_user_created_at ON orders (user_id, created_at DESC);  -- "my orders, newest first"
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items (product_id);

CREATE OR REPLACE TRIGGER trg_carts_updated_at
    BEFORE UPDATE ON carts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============ PAYMENT_DB ============

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS payment_methods (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL,  -- references user_db.users(id)
    type             VARCHAR(20)  NOT NULL,
    card_token       VARCHAR(255) NOT NULL,  -- gateway token, never a raw card number
    last_four_digits CHAR(4),
    is_default       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_payment_methods_type
        CHECK (type IN ('CREDIT_CARD', 'DEBIT_CARD', 'PAYPAL', 'BANK_TRANSFER')),
    CONSTRAINT chk_payment_methods_last_four
        CHECK (last_four_digits IS NULL OR last_four_digits ~ '^[0-9]{4}$'),
    CONSTRAINT uk_payment_methods_card_token UNIQUE (card_token)
);

CREATE TABLE IF NOT EXISTS payments (
    id                BIGSERIAL PRIMARY KEY,
    order_id          BIGINT         NOT NULL,  -- references order_db.orders(id)
    user_id           BIGINT         NOT NULL,  -- references user_db.users(id)
    amount            NUMERIC(12, 2) NOT NULL,
    status            VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    transaction_id    VARCHAR(255),             -- set by the gateway once processed
    payment_method_id BIGINT         REFERENCES payment_methods(id) ON DELETE SET NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_payments_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    CONSTRAINT chk_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT uk_payments_order          UNIQUE (order_id),
    CONSTRAINT uk_payments_transaction    UNIQUE (transaction_id)  -- NULLs allowed, duplicates not
);

CREATE TABLE IF NOT EXISTS payment_status_history (
    id          BIGSERIAL PRIMARY KEY,
    payment_id  BIGINT       NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    from_status VARCHAR(20),              -- NULL for the initial transition
    to_status   VARCHAR(20)  NOT NULL,
    changed_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by  VARCHAR(255),             -- user, service or system actor
    reason      TEXT,

    CONSTRAINT chk_status_history_from
        CHECK (from_status IS NULL
               OR from_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    CONSTRAINT chk_status_history_to
        CHECK (to_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED'))
);

CREATE INDEX IF NOT EXISTS idx_payment_methods_user_id   ON payment_methods (user_id);
-- At most one default payment method per user.
CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_methods_user_default
    ON payment_methods (user_id) WHERE is_default;
-- payments.order_id and transaction_id are covered by their unique constraints.
CREATE INDEX IF NOT EXISTS idx_payments_user_id          ON payments (user_id);
CREATE INDEX IF NOT EXISTS idx_payments_status           ON payments (status);
CREATE INDEX IF NOT EXISTS idx_payments_payment_method   ON payments (payment_method_id);
CREATE INDEX IF NOT EXISTS idx_status_history_payment    ON payment_status_history (payment_id, changed_at);

CREATE OR REPLACE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============ NOTIFICATION_DB ============

CREATE TABLE IF NOT EXISTS notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL,  -- references user_db.users(id)
    type       VARCHAR(20)  NOT NULL,
    title      VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at    TIMESTAMP,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_notifications_type
        CHECK (type IN ('ORDER', 'PAYMENT', 'SHIPPING', 'PROMOTION', 'SYSTEM'))
);

CREATE TABLE IF NOT EXISTS email_logs (
    id              BIGSERIAL PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    content         TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    sent_at         TIMESTAMP,
    error_message   TEXT,
    retry_count     INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_email_logs_status
        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'BOUNCED')),
    CONSTRAINT chk_email_logs_retry_non_negative CHECK (retry_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id      ON notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_type         ON notifications (type);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read      ON notifications (is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at   ON notifications (created_at);
-- "Unread notifications for this user" - the most common inbox query.
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notifications (user_id, created_at DESC) WHERE NOT is_read;
CREATE INDEX IF NOT EXISTS idx_email_logs_status          ON email_logs (status);
CREATE INDEX IF NOT EXISTS idx_email_logs_recipient       ON email_logs (recipient_email);
CREATE INDEX IF NOT EXISTS idx_email_logs_created_at      ON email_logs (created_at);

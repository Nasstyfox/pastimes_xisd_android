-- ============================================================
-- Pastimes — MySQL Schema + Seed Data
-- Database: Pastimes
-- Engine: InnoDB | Charset: utf8mb4
-- ============================================================

USE Pastimes;

-- ============================================================
-- DROP (reverse FK order — safe to re-run)
-- ============================================================
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS carts;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS user_addresses;
DROP TABLE IF EXISTS user_settings;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. USERS
-- ============================================================
CREATE TABLE users (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    firebase_uid    VARCHAR(128)    NOT NULL,
    full_name       VARCHAR(120)    NOT NULL,
    email           VARCHAR(160)    NOT NULL,
    phone           VARCHAR(20)     NULL,
    role            ENUM('buyer','seller','admin') NOT NULL,
    auth_provider   ENUM('password','google')      NOT NULL DEFAULT 'password',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_firebase_uid (firebase_uid),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_role (role)
) ENGINE=InnoDB;

-- ============================================================
-- 2. USER SETTINGS
-- ============================================================
CREATE TABLE user_settings (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id               BIGINT UNSIGNED NOT NULL,
    theme                 ENUM('light','dark','system') NOT NULL DEFAULT 'system',
    notifications_enabled TINYINT(1) NOT NULL DEFAULT 1,
    language              VARCHAR(10) NOT NULL DEFAULT 'en',
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                          ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_settings_user (user_id),
    CONSTRAINT fk_settings_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 3. USER ADDRESSES
-- ============================================================
CREATE TABLE user_addresses (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id      BIGINT UNSIGNED NOT NULL,
    label        VARCHAR(60)  NULL,
    recipient    VARCHAR(120) NOT NULL,
    phone        VARCHAR(20)  NOT NULL,
    street       VARCHAR(180) NOT NULL,
    suburb       VARCHAR(120) NOT NULL,
    city         VARCHAR(120) NOT NULL,
    province     VARCHAR(120) NOT NULL,
    postal_code  VARCHAR(12)  NOT NULL,
    country      VARCHAR(80)  NOT NULL DEFAULT 'South Africa',
    is_default   TINYINT(1)   NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_addr_user (user_id),
    CONSTRAINT fk_addr_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 4. CATEGORIES
-- ============================================================
CREATE TABLE categories (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255) NULL,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_categories_name (name)
) ENGINE=InnoDB;

-- ============================================================
-- 5. ITEMS
-- ============================================================
CREATE TABLE items (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    seller_id     BIGINT UNSIGNED NOT NULL,
    category_id   BIGINT UNSIGNED NOT NULL,
    title         VARCHAR(150)  NOT NULL,
    description   TEXT          NULL,
    price         DECIMAL(10,2) NOT NULL,
    size          VARCHAR(20)   NULL,
    brand         VARCHAR(80)   NULL,
    colour        VARCHAR(50)   NULL,
    condition_tag ENUM('new','like_new','good','fair') NOT NULL DEFAULT 'good',
    image_url     VARCHAR(500)  NULL,
    status        ENUM('available','reserved','sold','removed')
                  NOT NULL DEFAULT 'available',
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                  ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_items_seller   (seller_id),
    KEY idx_items_category (category_id),
    KEY idx_items_status   (status),
    CONSTRAINT fk_items_seller FOREIGN KEY (seller_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_items_category FOREIGN KEY (category_id)
        REFERENCES categories(id)
) ENGINE=InnoDB;

-- ============================================================
-- 6. CARTS
-- ============================================================
CREATE TABLE carts (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    buyer_id   BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
               ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_carts_buyer (buyer_id),
    CONSTRAINT fk_carts_buyer FOREIGN KEY (buyer_id)
        REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 7. CART ITEMS
-- ============================================================
CREATE TABLE cart_items (
    id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    cart_id   BIGINT UNSIGNED NOT NULL,
    item_id   BIGINT UNSIGNED NOT NULL,
    added_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_cart_item (cart_id, item_id),
    CONSTRAINT fk_cartitems_cart FOREIGN KEY (cart_id)
        REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cartitems_item FOREIGN KEY (item_id)
        REFERENCES items(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 8. ORDERS
-- ============================================================
CREATE TABLE orders (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    buyer_id          BIGINT UNSIGNED NOT NULL,
    total_amount      DECIMAL(10,2) NOT NULL,
    status            ENUM('pending','paid','shipped','delivered','cancelled')
                      NOT NULL DEFAULT 'pending',
    ship_recipient    VARCHAR(120) NOT NULL,
    ship_phone        VARCHAR(20)  NOT NULL,
    ship_street       VARCHAR(180) NOT NULL,
    ship_suburb       VARCHAR(120) NOT NULL,
    ship_city         VARCHAR(120) NOT NULL,
    ship_province     VARCHAR(120) NOT NULL,
    ship_postal_code  VARCHAR(12)  NOT NULL,
    ship_country      VARCHAR(80)  NOT NULL DEFAULT 'South Africa',
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                      ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_orders_buyer  (buyer_id),
    KEY idx_orders_status (status),
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id)
        REFERENCES users(id)
) ENGINE=InnoDB;

-- ============================================================
-- 9. ORDER ITEMS
-- ============================================================
CREATE TABLE order_items (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id          BIGINT UNSIGNED NOT NULL,
    item_id           BIGINT UNSIGNED NOT NULL,
    seller_id         BIGINT UNSIGNED NOT NULL,
    price_at_purchase DECIMAL(10,2) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_oitems_order  (order_id),
    KEY idx_oitems_seller (seller_id),
    CONSTRAINT fk_oitems_order  FOREIGN KEY (order_id)
        REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_oitems_item   FOREIGN KEY (item_id)
        REFERENCES items(id),
    CONSTRAINT fk_oitems_seller FOREIGN KEY (seller_id)
        REFERENCES users(id)
) ENGINE=InnoDB;

-- ============================================================
-- 10. AUDIT LOGS
-- ============================================================
CREATE TABLE audit_logs (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    admin_id       BIGINT UNSIGNED NOT NULL,
    action         VARCHAR(80)  NOT NULL,
    target_user_id BIGINT UNSIGNED NULL,
    details        VARCHAR(500) NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_admin  (admin_id),
    KEY idx_audit_target (target_user_id),
    CONSTRAINT fk_audit_admin FOREIGN KEY (admin_id)
        REFERENCES users(id),
    CONSTRAINT fk_audit_target FOREIGN KEY (target_user_id)
        REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- SEED: 2 USERS (1 buyer + 1 seller)
-- ============================================================
--
-- IMPORTANT: firebase_uid MUST match a real Firebase user's UID,
-- otherwise /api/auth/me will reject the token.
--
-- Replace the two values below with the actual UIDs from:
--   Firebase Console → Authentication → Users → User UID column
--
-- You can also get them by signing up buyer@pastimes.com and
-- seller@pastimes.com in the app — Firebase generates the UIDs,
-- then run INSERT ... SELECT to copy them into MySQL.
--
-- The values below are PLACEHOLDERS for structural demonstration.
-- ============================================================

INSERT INTO users
    (firebase_uid, full_name, email, phone, role, auth_provider)
VALUES
    ('REPLACE_BUYER_FIREBASE_UID_0001',
     'Thandi Mokoena',
     'buyer@pastimes.com',
     '0721234567',
     'buyer',
     'password'),

    ('REPLACE_SELLER_FIREBASE_UID_0002',
     'Sipho Ndlovu',
     'seller@pastimes.com',
     '0739876543',
     'seller',
     'password');

-- Settings rows (one per user)
INSERT INTO user_settings (user_id, theme, notifications_enabled, language) VALUES
    (1, 'system', 1, 'en'),
    (2, 'system', 1, 'en');

-- Cart for the buyer only
INSERT INTO carts (buyer_id) VALUES (1);

-- ============================================================
-- SEED: 10 CATEGORIES
-- ============================================================
INSERT INTO categories (name, description) VALUES
    ('Tops',        'T-shirts, shirts, blouses, sweaters'),
    ('Bottoms',     'Jeans, trousers, skirts, shorts'),
    ('Dresses',     'Casual and formal dresses'),
    ('Outerwear',   'Jackets, coats, blazers'),
    ('Shoes',       'Sneakers, boots, sandals, heels'),
    ('Bags',        'Handbags, backpacks, totes'),
    ('Accessories', 'Belts, scarves, hats, sunglasses'),
    ('Sportswear',  'Activewear and gym clothing'),
    ('Kids',        'Children''s clothing'),
    ('Formal',      'Suits, evening wear, formal attire');

-- ============================================================
-- SEED: 6 ITEMS listed by the seller (user id = 2)
-- ============================================================
INSERT INTO items
    (seller_id, category_id, title, description, price,
     size, brand, colour, condition_tag, image_url, status)
VALUES
    (2, 1, 'Vintage Denim Jacket',
     'Classic 90s Levi''s denim jacket, minimal wear',
     349.99, 'M', 'Levi''s', 'Blue', 'good',
     'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
     'available'),

    (2, 2, 'Slim Fit Jeans',
     'Dark indigo slim fit, worn twice',
     299.00, '32', 'Levi''s', 'Indigo', 'like_new',
     'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
     'available'),

    (2, 4, 'Leather Biker Jacket',
     'Black leather, quilted shoulders, YKK zips',
     850.00, 'L', 'Zara', 'Black', 'like_new',
     'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
     'available'),

    (2, 5, 'Nike Air Max 90',
     'White with grey accents, light creasing',
     650.00, '9', 'Nike', 'White', 'good',
     'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
     'available'),

    (2, 3, 'Summer Floral Dress',
     'Brand new with tags, zip back, midi length',
     220.00, 'S', 'H&M', 'Floral', 'new',
     'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
     'available'),

    (2, 6, 'Canvas Tote Bag',
     'Beige canvas, fits a 15" laptop',
     150.00, 'One Size', 'Uniqlo', 'Beige', 'good',
     'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
     'available');

-- ============================================================
-- VERIFY
-- ============================================================
SELECT 'users'         AS table_name, COUNT(*) AS rows_count FROM users
UNION ALL SELECT 'user_settings',  COUNT(*) FROM user_settings
UNION ALL SELECT 'user_addresses', COUNT(*) FROM user_addresses
UNION ALL SELECT 'categories',     COUNT(*) FROM categories
UNION ALL SELECT 'items',          COUNT(*) FROM items
UNION ALL SELECT 'carts',          COUNT(*) FROM carts
UNION ALL SELECT 'cart_items',     COUNT(*) FROM cart_items
UNION ALL SELECT 'orders',         COUNT(*) FROM orders
UNION ALL SELECT 'order_items',    COUNT(*) FROM order_items
UNION ALL SELECT 'audit_logs',     COUNT(*) FROM audit_logs;

UPDATE Pastimes.users
   SET firebase_uid = 'PASTE_REAL_BUYER_UID'
 WHERE email = 'buyer@pastimes.com';

UPDATE Pastimes.users
   SET firebase_uid = 'PASTE_REAL_SELLER_UID'
 WHERE email = 'seller@pastimes.com';
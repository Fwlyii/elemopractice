-- SRS 增量表。可重复执行，不删除现有业务数据。
SET NAMES utf8mb4;
-- 修复历史上被错误按 latin1 写入的地址文本；通过特征字符判断，重复执行不会影响正常中文。

-- 订单幂等键：同一顾客重复提交同一请求只返回原订单。
SET @idempotency_key_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'idempotency_key');
SET @idempotency_key_sql := IF(@idempotency_key_exists = 0,
  'ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(64) NULL COMMENT ''顾客提交订单幂等键''',
  'SELECT 1');
PREPARE idempotency_key_stmt FROM @idempotency_key_sql;
EXECUTE idempotency_key_stmt;
DEALLOCATE PREPARE idempotency_key_stmt;
SET @idempotency_index_exists := (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'orders' AND index_name = 'uk_orders_customer_idempotency');
SET @idempotency_index_sql := IF(@idempotency_index_exists = 0,
  'ALTER TABLE orders ADD UNIQUE KEY uk_orders_customer_idempotency (customer_id, idempotency_key)',
  'SELECT 1');
PREPARE idempotency_index_stmt FROM @idempotency_index_sql;
EXECUTE idempotency_index_stmt;
DEALLOCATE PREPARE idempotency_index_stmt;
SET @service_mode_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'service_mode');
SET @service_mode_sql := IF(@service_mode_exists = 0,
  'ALTER TABLE orders ADD COLUMN service_mode VARCHAR(16) NOT NULL DEFAULT ''DELIVERY'' COMMENT ''履约方式：外送或自取''',
  'SELECT 1');
PREPARE service_mode_stmt FROM @service_mode_sql;
EXECUTE service_mode_stmt;
DEALLOCATE PREPARE service_mode_stmt;
UPDATE orders SET service_mode = 'DELIVERY' WHERE service_mode IS NULL OR service_mode = '';
UPDATE delivery_address
SET address = CONVERT(CAST(CONVERT(address USING latin1) AS BINARY) USING utf8mb4)
WHERE address REGEXP '[åæçèé]';
UPDATE orders
SET address_snapshot = CONVERT(CAST(CONVERT(address_snapshot USING latin1) AS BINARY) USING utf8mb4)
WHERE address_snapshot REGEXP '[åæçèé]';

-- 自取订单不需要收货地址；历史表结构可能仍将 address_id 设为 NOT NULL。
SET @order_address_nullable_sql := (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE orders MODIFY COLUMN address_id BIGINT NULL',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'orders'
    AND column_name = 'address_id' AND is_nullable = 'NO'
);
PREPARE order_address_nullable_stmt FROM @order_address_nullable_sql;
EXECUTE order_address_nullable_stmt;
DEALLOCATE PREPARE order_address_nullable_stmt;

-- 商品库存：历史库没有该列时补充，默认每个新商品100份。
SET @stock_column_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'stock');
SET @stock_alter_sql := IF(@stock_column_exists = 0,
  'ALTER TABLE food ADD COLUMN stock INT NOT NULL DEFAULT 100 COMMENT ''可售库存''',
  'SELECT 1');
PREPARE stock_stmt FROM @stock_alter_sql;
EXECUTE stock_stmt;
DEALLOCATE PREPARE stock_stmt;

-- 商品经营属性：分类用于菜单分组，单笔限购由后端在加购和下单时双重校验。
SET @food_category_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'category');
SET @food_category_sql := IF(@food_category_exists = 0,
  'ALTER TABLE food ADD COLUMN category VARCHAR(32) CHARACTER SET utf8mb4 NOT NULL DEFAULT _utf8mb4 0xE68B9BE7898CE68EA8E88D90 COMMENT ''商品分类''',
  'SELECT 1');
PREPARE food_category_stmt FROM @food_category_sql;
EXECUTE food_category_stmt;
DEALLOCATE PREPARE food_category_stmt;

SET @purchase_limit_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'purchase_limit');
SET @purchase_limit_sql := IF(@purchase_limit_exists = 0,
  'ALTER TABLE food ADD COLUMN purchase_limit INT NULL COMMENT ''单笔限购数量，空表示不限购''',
  'SELECT 1');
PREPARE purchase_limit_stmt FROM @purchase_limit_sql;
EXECUTE purchase_limit_stmt;
DEALLOCATE PREPARE purchase_limit_stmt;
UPDATE food SET category = CONVERT(X'E68B9BE7898CE68EA8E88D90' USING utf8mb4) WHERE category IS NULL OR TRIM(category) = '';
UPDATE food SET purchase_limit = NULL WHERE purchase_limit IS NOT NULL AND purchase_limit <= 0;

-- 门店经营标签配置：由商家维护堂食能力和一档满减规则，首页按数据阈值生成标签。
SET @dine_in_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'business' AND column_name = 'dine_in_available');
SET @dine_in_sql := IF(@dine_in_exists = 0,
  'ALTER TABLE business ADD COLUMN dine_in_available TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否支持堂食''',
  'SELECT 1');
PREPARE dine_in_stmt FROM @dine_in_sql;
EXECUTE dine_in_stmt;
DEALLOCATE PREPARE dine_in_stmt;

SET @promotion_threshold_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'business' AND column_name = 'promotion_threshold');
SET @promotion_threshold_sql := IF(@promotion_threshold_exists = 0,
  'ALTER TABLE business ADD COLUMN promotion_threshold DECIMAL(10,2) NULL COMMENT ''满减门槛''',
  'SELECT 1');
PREPARE promotion_threshold_stmt FROM @promotion_threshold_sql;
EXECUTE promotion_threshold_stmt;
DEALLOCATE PREPARE promotion_threshold_stmt;

SET @promotion_discount_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'business' AND column_name = 'promotion_discount');
SET @promotion_discount_sql := IF(@promotion_discount_exists = 0,
  'ALTER TABLE business ADD COLUMN promotion_discount DECIMAL(10,2) NULL COMMENT ''满减优惠金额''',
  'SELECT 1');
PREPARE promotion_discount_stmt FROM @promotion_discount_sql;
EXECUTE promotion_discount_stmt;
DEALLOCATE PREPARE promotion_discount_stmt;

-- 首页展示快照：仅用于演示数据的评分和近30日销量；真实商家没有快照时仍由订单/互动数据聚合。
SET @demo_rating_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'business' AND column_name = 'demo_rating');
SET @demo_rating_sql := IF(@demo_rating_exists = 0,
  'ALTER TABLE business ADD COLUMN demo_rating DECIMAL(3,2) NULL COMMENT ''演示用评分快照，真实业务由评价聚合计算''',
  'SELECT 1');
PREPARE demo_rating_stmt FROM @demo_rating_sql;
EXECUTE demo_rating_stmt;
DEALLOCATE PREPARE demo_rating_stmt;

SET @demo_sales_count_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'business' AND column_name = 'demo_sales_count');
SET @demo_sales_count_sql := IF(@demo_sales_count_exists = 0,
  'ALTER TABLE business ADD COLUMN demo_sales_count INT NULL COMMENT ''演示用近30日销量快照，真实业务由订单聚合计算''',
  'SELECT 1');
PREPARE demo_sales_count_stmt FROM @demo_sales_count_sql;
EXECUTE demo_sales_count_stmt;
DEALLOCATE PREPARE demo_sales_count_stmt;

-- 支付资产结算字段：记录支付方式、积分抵扣和钱包扣款，便于取消时准确反向流水。
SET @payment_method_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'payment_method');
SET @payment_method_sql := IF(@payment_method_exists = 0,
  'ALTER TABLE orders ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT ''SIMULATED'' COMMENT ''支付方式''',
  'SELECT 1');
PREPARE payment_method_stmt FROM @payment_method_sql;
EXECUTE payment_method_stmt;
DEALLOCATE PREPARE payment_method_stmt;

SET @points_used_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'points_used');
SET @points_used_sql := IF(@points_used_exists = 0,
  'ALTER TABLE orders ADD COLUMN points_used INT NOT NULL DEFAULT 0 COMMENT ''本单抵扣积分''',
  'SELECT 1');
PREPARE points_used_stmt FROM @points_used_sql;
EXECUTE points_used_stmt;
DEALLOCATE PREPARE points_used_stmt;

SET @wallet_paid_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'wallet_paid');
SET @wallet_paid_sql := IF(@wallet_paid_exists = 0,
  'ALTER TABLE orders ADD COLUMN wallet_paid TINYINT NOT NULL DEFAULT 0 COMMENT ''是否钱包支付''',
  'SELECT 1');
PREPARE wallet_paid_stmt FROM @wallet_paid_sql;
EXECUTE wallet_paid_stmt;
DEALLOCATE PREPARE wallet_paid_stmt;

SET @payment_status_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'payment_status');
SET @payment_status_sql := IF(@payment_status_exists = 0,
  'ALTER TABLE orders ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT ''PENDING'' COMMENT ''支付状态''',
  'SELECT 1');
PREPARE payment_status_stmt FROM @payment_status_sql;
EXECUTE payment_status_stmt;
DEALLOCATE PREPARE payment_status_stmt;

SET @address_snapshot_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'address_snapshot');
SET @address_snapshot_sql := IF(@address_snapshot_exists = 0,
  'ALTER TABLE orders ADD COLUMN address_snapshot VARCHAR(255) NULL COMMENT ''下单地址快照''',
  'SELECT 1');
PREPARE address_snapshot_stmt FROM @address_snapshot_sql;
EXECUTE address_snapshot_stmt;
DEALLOCATE PREPARE address_snapshot_stmt;

SET @contact_name_snapshot_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'contact_name_snapshot');
SET @contact_name_snapshot_sql := IF(@contact_name_snapshot_exists = 0,
  'ALTER TABLE orders ADD COLUMN contact_name_snapshot VARCHAR(80) NULL',
  'SELECT 1');
PREPARE contact_name_snapshot_stmt FROM @contact_name_snapshot_sql;
EXECUTE contact_name_snapshot_stmt;
DEALLOCATE PREPARE contact_name_snapshot_stmt;

SET @contact_sex_snapshot_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'contact_sex_snapshot');
SET @contact_sex_snapshot_sql := IF(@contact_sex_snapshot_exists = 0,
  'ALTER TABLE orders ADD COLUMN contact_sex_snapshot TINYINT NULL',
  'SELECT 1');
PREPARE contact_sex_snapshot_stmt FROM @contact_sex_snapshot_sql;
EXECUTE contact_sex_snapshot_stmt;
DEALLOCATE PREPARE contact_sex_snapshot_stmt;

SET @contact_tel_snapshot_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'contact_tel_snapshot');
SET @contact_tel_snapshot_sql := IF(@contact_tel_snapshot_exists = 0,
  'ALTER TABLE orders ADD COLUMN contact_tel_snapshot VARCHAR(30) NULL',
  'SELECT 1');
PREPARE contact_tel_snapshot_stmt FROM @contact_tel_snapshot_sql;
EXECUTE contact_tel_snapshot_stmt;
DEALLOCATE PREPARE contact_tel_snapshot_stmt;

CREATE TABLE IF NOT EXISTS review (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  business_id BIGINT NOT NULL,
  rating TINYINT NOT NULL,
  content VARCHAR(500) NOT NULL DEFAULT '',
  images VARCHAR(1000) NULL,
  merchant_reply VARCHAR(500) NULL,
  reply_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_review_order (order_id),
  KEY idx_review_business (business_id, create_time),
  CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_review_customer FOREIGN KEY (customer_id) REFERENCES users(id),
  CONSTRAINT fk_review_business FOREIGN KEY (business_id) REFERENCES business(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @review_hidden_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'review' AND column_name = 'is_hidden');
SET @review_hidden_sql := IF(@review_hidden_exists = 0,
  'ALTER TABLE review ADD COLUMN is_hidden TINYINT NOT NULL DEFAULT 0 COMMENT ''管理员隐藏标记''',
  'SELECT 1');
PREPARE review_hidden_stmt FROM @review_hidden_sql;
EXECUTE review_hidden_stmt;
DEALLOCATE PREPARE review_hidden_stmt;

CREATE TABLE IF NOT EXISTS user_asset (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  balance DECIMAL(10,2) NOT NULL DEFAULT 0,
  points INT NOT NULL DEFAULT 0,
  membership_expire DATETIME NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id), UNIQUE KEY uk_asset_user(user_id),
  CONSTRAINT fk_asset_user FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_coupon (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(80) NOT NULL,
  discount_amount DECIMAL(10,2) NOT NULL,
  min_order_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  expires_at DATETIME NOT NULL,
  used TINYINT NOT NULL DEFAULT 0,
  order_id BIGINT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id), KEY idx_coupon_user(user_id, used, expires_at),
  CONSTRAINT fk_coupon_user FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_preference (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  theme VARCHAR(16) NOT NULL DEFAULT 'light',
  spicy_level TINYINT NOT NULL DEFAULT 0,
  taste_tags VARCHAR(200) NOT NULL DEFAULT '',
  avoid_tags VARCHAR(200) NOT NULL DEFAULT '',
  category_tags VARCHAR(200) NOT NULL DEFAULT '',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY(id), UNIQUE KEY uk_preference_user(user_id),
  CONSTRAINT fk_preference_user FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_asset_ledger (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  points_delta INT NOT NULL DEFAULT 0,
  reason VARCHAR(255) NOT NULL,
  reference_id BIGINT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id), KEY idx_asset_ledger_user_time(user_id, create_time),
  CONSTRAINT fk_asset_ledger_user FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

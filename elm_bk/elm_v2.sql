/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80040 (8.0.40)
 Source Host           : localhost:3306
 Source Schema         : elm_v2

 Target Server Type    : MySQL
 Target Server Version : 80040 (8.0.40)
 File Encoding         : 65001

 Date: 26/09/2025 19:58:08
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_chat_history
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_history`;
CREATE TABLE `ai_chat_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `creator` bigint NULL DEFAULT NULL COMMENT '创建者ID',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除(0:否,1:是)',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `updater` bigint NULL DEFAULT NULL COMMENT '更新者ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话ID',
  `user_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户消息',
  `ai_response` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'AI回复',
  `chat_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'general' COMMENT '对话类型',
  `processing_time` bigint NULL DEFAULT NULL COMMENT '处理耗时(毫秒)',
  `context_data` json NULL COMMENT '上下文数据',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE COMMENT '用户ID索引',
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE COMMENT '会话ID索引',
  INDEX `idx_chat_type`(`chat_type` ASC) USING BTREE COMMENT '对话类型索引',
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE COMMENT '创建时间索引',
  CONSTRAINT `fk_ai_chat_history_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI对话历史表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_chat_history
-- ----------------------------

-- ----------------------------
-- Table structure for authority
-- ----------------------------
DROP TABLE IF EXISTS `authority`;
CREATE TABLE `authority`  (
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of authority
-- ----------------------------
INSERT INTO `authority` VALUES ('ADMIN');
INSERT INTO `authority` VALUES ('BUSINESS');
INSERT INTO `authority` VALUES ('RIDER');
INSERT INTO `authority` VALUES ('USER');

-- ----------------------------
-- Table structure for business
-- ----------------------------
DROP TABLE IF EXISTS `business`;
CREATE TABLE `business`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` timestamp NULL DEFAULT NULL,
  `creator` bigint NULL DEFAULT NULL,
  `is_deleted` tinyint(1) UNSIGNED ZEROFILL NULL DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT NULL,
  `updater` bigint NULL DEFAULT NULL,
  `business_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `business_explain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `business_img` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `business_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `delivery_price` decimal(10, 2) NULL DEFAULT NULL,
  `order_type_id` int NULL DEFAULT NULL,
  `remarks` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `start_price` decimal(10, 2) NULL DEFAULT NULL,
  `dine_in_available` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否支持堂食',
  `promotion_threshold` decimal(10, 2) NULL DEFAULT NULL COMMENT '满减门槛',
  `promotion_discount` decimal(10, 2) NULL DEFAULT NULL COMMENT '满减优惠金额',
  `demo_rating` decimal(3, 2) NULL DEFAULT NULL COMMENT '演示用评分快照，真实业务由评价聚合计算',
  `demo_sales_count` int NULL DEFAULT NULL COMMENT '演示用近30日销量快照，真实业务由订单聚合计算',
  `user_id` bigint NOT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-待审核 1-已上线 2-被拒绝',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `business_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of business
-- ----------------------------

-- ----------------------------
-- Table structure for cart
-- ----------------------------
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` timestamp NULL DEFAULT NULL,
  `creator` bigint NULL DEFAULT NULL,
  `is_deleted` tinyint(1) NULL DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT NULL,
  `updater` bigint NULL DEFAULT NULL,
  `quantity` int NULL DEFAULT NULL,
  `business_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `food_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `business_id`(`business_id` ASC) USING BTREE,
  INDEX `customer_id`(`customer_id` ASC) USING BTREE,
  INDEX `food_id`(`food_id` ASC) USING BTREE,
  CONSTRAINT `cart_ibfk_1` FOREIGN KEY (`business_id`) REFERENCES `business` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `cart_ibfk_2` FOREIGN KEY (`customer_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `cart_ibfk_3` FOREIGN KEY (`food_id`) REFERENCES `food` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cart
-- ----------------------------

-- ----------------------------
-- Table structure for delivery_address
-- ----------------------------
DROP TABLE IF EXISTS `delivery_address`;
CREATE TABLE `delivery_address`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` timestamp NULL DEFAULT NULL,
  `creator` bigint NULL DEFAULT NULL,
  `is_deleted` tinyint(1) NULL DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT NULL,
  `updater` bigint NULL DEFAULT NULL,
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `contact_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `contact_sex` int NULL DEFAULT NULL,
  `contact_tel` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `delivery_address_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of delivery_address
-- ----------------------------

-- ----------------------------
-- Table structure for food
-- ----------------------------
DROP TABLE IF EXISTS `food`;
CREATE TABLE `food`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` timestamp NULL DEFAULT NULL,
  `creator` bigint NULL DEFAULT NULL,
  `is_deleted` tinyint(1) NULL DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT NULL,
  `updater` bigint NULL DEFAULT NULL,
  `food_explain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `food_img` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `food_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `food_price` decimal(10, 2) NOT NULL,
  `remarks` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `business_id` bigint NOT NULL,
  `shelve_status` tinyint NULL DEFAULT 1 COMMENT '0-已下架 1-已上架',
  `stock` int NOT NULL DEFAULT 100 COMMENT '可售库存',
  `category` varchar(32) NOT NULL DEFAULT '招牌推荐' COMMENT '商品分类',
  `purchase_limit` int NULL DEFAULT NULL COMMENT '单笔限购数量，空表示不限购',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `business_id`(`business_id` ASC) USING BTREE,
  CONSTRAINT `food_ibfk_1` FOREIGN KEY (`business_id`) REFERENCES `business` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of food
-- ----------------------------

-- ----------------------------
-- Table structure for merchant_interaction
-- ----------------------------
DROP TABLE IF EXISTS `merchant_interaction`;
CREATE TABLE `merchant_interaction`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `merchant_id` bigint NOT NULL COMMENT '商铺ID',
  `liked` tinyint(1) NULL DEFAULT 0 COMMENT '是否点赞(0:否,1:是)',
  `collected` tinyint(1) NULL DEFAULT 0 COMMENT '是否收藏(0:否,1:是)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_merchant`(`user_id` ASC, `merchant_id` ASC) USING BTREE COMMENT '用户商家唯一索引',
  INDEX `idx_merchant_id`(`merchant_id` ASC) USING BTREE COMMENT '商家ID索引',
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE COMMENT '用户ID索引',
  INDEX `idx_liked`(`liked` ASC) USING BTREE COMMENT '点赞状态索引',
  INDEX `idx_collected`(`collected` ASC) USING BTREE COMMENT '收藏状态索引',
  CONSTRAINT `fk_interaction_business` FOREIGN KEY (`merchant_id`) REFERENCES `business` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_interaction_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商家互动表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of merchant_interaction
-- ----------------------------

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息唯一ID',
  `user_id` bigint NOT NULL COMMENT '接收用户ID（关联users表id）',
  `notification_type` tinyint NOT NULL COMMENT '消息类型：0=商家申请审核，1=开店申请审核',
  `notification_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息内容',
  `audit_result` tinyint NOT NULL COMMENT '审核结果：1=通过，2=拒绝',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '阅读状态：0=未读，1=已读',
  `create_time` datetime NOT NULL COMMENT '消息创建时间',
  `read_time` datetime NULL DEFAULT NULL COMMENT '消息阅读时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_is_read`(`user_id` ASC, `is_read` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户消息通知表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of notification
-- ----------------------------

-- ----------------------------
-- Table structure for orderdetailet
-- ----------------------------
DROP TABLE IF EXISTS `orderdetailet`;
CREATE TABLE `orderdetailet`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` timestamp NULL DEFAULT NULL,
  `creator` bigint NULL DEFAULT NULL,
  `is_deleted` tinyint(1) NULL DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT NULL,
  `updater` bigint NULL DEFAULT NULL,
  `quantity` int NULL DEFAULT NULL,
  `food_id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `food_price` decimal(10, 2) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `food_id`(`food_id` ASC) USING BTREE,
  INDEX `order_id`(`order_id` ASC) USING BTREE,
  CONSTRAINT `orderdetailet_ibfk_1` FOREIGN KEY (`food_id`) REFERENCES `food` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `orderdetailet_ibfk_2` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of orderdetailet
-- ----------------------------

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` timestamp NULL DEFAULT NULL,
  `creator` bigint NULL DEFAULT NULL,
  `is_deleted` tinyint(1) NULL DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT NULL,
  `updater` bigint NULL DEFAULT NULL,
  `order_date` timestamp NULL DEFAULT NULL,
  `order_state` int NULL DEFAULT NULL,
  `order_total` decimal(10, 2) NOT NULL,
  `business_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `address_id` bigint NULL COMMENT '外送地址；自取订单为空',
  `delivery_price` decimal(10, 2) NOT NULL,
  `payment_method` varchar(20) NOT NULL DEFAULT 'SIMULATED' COMMENT '支付方式',
  `points_used` int NOT NULL DEFAULT 0 COMMENT '本单抵扣积分',
  `wallet_paid` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否钱包支付',
  `payment_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '支付状态',
  `idempotency_key` varchar(64) DEFAULT NULL COMMENT '顾客提交订单幂等键',
  `service_mode` varchar(16) NOT NULL DEFAULT 'DELIVERY' COMMENT '履约方式：外送或自取',
  `address_snapshot` varchar(255) DEFAULT NULL COMMENT '下单地址快照',
  `contact_name_snapshot` varchar(80) DEFAULT NULL,
  `contact_sex_snapshot` tinyint DEFAULT NULL,
  `contact_tel_snapshot` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `business_id`(`business_id` ASC) USING BTREE,
  INDEX `customer_id`(`customer_id` ASC) USING BTREE,
  INDEX `address_id`(`address_id` ASC) USING BTREE,
  UNIQUE KEY `uk_orders_customer_idempotency` (`customer_id`,`idempotency_key`),
  CONSTRAINT `orders_ibfk_1` FOREIGN KEY (`business_id`) REFERENCES `business` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `orders_ibfk_2` FOREIGN KEY (`customer_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `orders_ibfk_3` FOREIGN KEY (`address_id`) REFERENCES `delivery_address` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of orders
-- ----------------------------

-- ----------------------------
-- Table structure for permission_application
-- ----------------------------
DROP TABLE IF EXISTS `permission_application`;
CREATE TABLE `permission_application`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `userId` bigint NOT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '审核状态 0-未审核 1-同意 2-拒绝',
  `is_deleted` tinyint NULL DEFAULT 0,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of permission_application
-- ----------------------------

-- ----------------------------
-- Table structure for person
-- ----------------------------
DROP TABLE IF EXISTS `person`;
CREATE TABLE `person`  (
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `first_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `gender` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `last_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `photo` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  CONSTRAINT `person_ibfk_1` FOREIGN KEY (`id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of person
-- ----------------------------

-- ----------------------------
-- Table structure for user_authority
-- ----------------------------
DROP TABLE IF EXISTS `user_authority`;
CREATE TABLE `user_authority`  (
  `user_id` bigint NOT NULL,
  `authority_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`user_id`, `authority_name`) USING BTREE,
  INDEX `authority_name`(`authority_name` ASC) USING BTREE,
  CONSTRAINT `user_authority_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `user_authority_ibfk_2` FOREIGN KEY (`authority_name`) REFERENCES `authority` (`name`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_authority
-- ----------------------------

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` timestamp NULL DEFAULT NULL,
  `creator` bigint NULL DEFAULT NULL,
  `is_deleted` tinyint(1) NULL DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT NULL,
  `updater` bigint NULL DEFAULT NULL,
  `activated` tinyint(1) NOT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of users
-- ----------------------------

-- ----------------------------
-- Table structure for rider_profile
-- ----------------------------
DROP TABLE IF EXISTS `rider_profile`;
CREATE TABLE `rider_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `real_name` varchar(50) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `vehicle_type` varchar(20) NOT NULL,
  `audit_status` tinyint NOT NULL DEFAULT 0 COMMENT '0-待审核 1-已通过 2-已拒绝',
  `online` tinyint(1) NOT NULL DEFAULT 0,
  `reject_reason` varchar(255) NULL,
  `completed_orders` int NOT NULL DEFAULT 0,
  `total_distance` decimal(10,2) NOT NULL DEFAULT 0,
  `total_income` decimal(10,2) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rider_user` (`user_id`),
  INDEX `idx_rider_audit_online` (`audit_status`, `online`),
  CONSTRAINT `fk_rider_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='骑手档案与资质审核';

-- ----------------------------
-- Table structure for delivery_task
-- ----------------------------
DROP TABLE IF EXISTS `delivery_task`;
CREATE TABLE `delivery_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `rider_user_id` bigint NULL,
  `task_status` varchar(32) NOT NULL,
  `version` int NOT NULL DEFAULT 0 COMMENT '抢单与状态流转的乐观锁版本',
  `distance_km` decimal(10,2) NOT NULL DEFAULT 0,
  `rider_fee` decimal(10,2) NOT NULL DEFAULT 0,
  `accepted_time` datetime NULL,
  `arrived_store_time` datetime NULL,
  `pickup_time` datetime NULL,
  `delivered_time` datetime NULL,
  `completed_time` datetime NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_delivery_order` (`order_id`),
  INDEX `idx_delivery_pool` (`task_status`, `rider_user_id`, `create_time`),
  INDEX `idx_delivery_rider` (`rider_user_id`, `task_status`),
  CONSTRAINT `fk_delivery_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_delivery_rider` FOREIGN KEY (`rider_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='配送任务';

-- ----------------------------
-- Table structure for delivery_exception
-- ----------------------------
DROP TABLE IF EXISTS `delivery_exception`;
CREATE TABLE `delivery_exception` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `rider_user_id` bigint NOT NULL,
  `exception_type` varchar(40) NOT NULL,
  `description` varchar(500) NOT NULL,
  `previous_task_status` varchar(32) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-待处理 1-已处理',
  `resolution_action` varchar(20) NULL,
  `resolution_note` varchar(500) NULL,
  `resolver_user_id` bigint NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `resolved_time` datetime NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_exception_status` (`status`, `create_time`),
  CONSTRAINT `fk_exception_task` FOREIGN KEY (`task_id`) REFERENCES `delivery_task` (`id`),
  CONSTRAINT `fk_exception_rider` FOREIGN KEY (`rider_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_exception_resolver` FOREIGN KEY (`resolver_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='配送异常工单';

-- ----------------------------
-- Table structure for order_status_history
-- ----------------------------
DROP TABLE IF EXISTS `order_status_history`;
CREATE TABLE `order_status_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `from_status` int NULL,
  `to_status` int NOT NULL,
  `operator_user_id` bigint NULL,
  `reason` varchar(255) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_order_history` (`order_id`, `create_time`),
  CONSTRAINT `fk_history_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_history_operator` FOREIGN KEY (`operator_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB CHARACTER SET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单履约状态轨迹';

-- SRS P1 增量表（与 srs-enhancements-migration.sql 保持一致）
CREATE TABLE IF NOT EXISTS `review` (
  `id` bigint NOT NULL AUTO_INCREMENT, `order_id` bigint NOT NULL, `customer_id` bigint NOT NULL,
  `business_id` bigint NOT NULL, `rating` tinyint NOT NULL, `content` varchar(500) NOT NULL DEFAULT '',
  `images` varchar(1000) DEFAULT NULL, `merchant_reply` varchar(500) DEFAULT NULL, `reply_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `is_hidden` tinyint NOT NULL DEFAULT 0 COMMENT '管理员隐藏标记',
  PRIMARY KEY (`id`), UNIQUE KEY `uk_review_order` (`order_id`), KEY `idx_review_business` (`business_id`,`create_time`),
  CONSTRAINT `fk_review_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_review_customer` FOREIGN KEY (`customer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_review_business` FOREIGN KEY (`business_id`) REFERENCES `business` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_asset` (
  `id` bigint NOT NULL AUTO_INCREMENT, `user_id` bigint NOT NULL,
  `balance` decimal(10,2) NOT NULL DEFAULT 0, `points` int NOT NULL DEFAULT 0,
  `membership_expire` datetime DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_asset_user` (`user_id`),
  CONSTRAINT `fk_asset_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT, `user_id` bigint NOT NULL, `name` varchar(80) NOT NULL,
  `discount_amount` decimal(10,2) NOT NULL, `min_order_amount` decimal(10,2) NOT NULL DEFAULT 0,
  `expires_at` datetime NOT NULL, `used` tinyint NOT NULL DEFAULT 0, `order_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), KEY `idx_coupon_user` (`user_id`,`used`,`expires_at`),
  CONSTRAINT `fk_coupon_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_preference` (
  `id` bigint NOT NULL AUTO_INCREMENT, `user_id` bigint NOT NULL,
  `theme` varchar(16) NOT NULL DEFAULT 'light', `spicy_level` tinyint NOT NULL DEFAULT 0,
  `taste_tags` varchar(200) NOT NULL DEFAULT '', `avoid_tags` varchar(200) NOT NULL DEFAULT '',
  `category_tags` varchar(200) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_preference_user` (`user_id`),
  CONSTRAINT `fk_preference_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_asset_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT, `user_id` bigint NOT NULL,
  `type` varchar(32) NOT NULL, `amount` decimal(10,2) NOT NULL DEFAULT 0,
  `points_delta` int NOT NULL DEFAULT 0, `reason` varchar(255) NOT NULL,
  `reference_id` bigint DEFAULT NULL, `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), KEY `idx_asset_ledger_user_time` (`user_id`,`create_time`),
  CONSTRAINT `fk_asset_ledger_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

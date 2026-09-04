USE elm_v2;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO users
  (id, create_time, is_deleted, activated, password, username)
VALUES
  (1, NOW(), 0, 1, '$2a$08$urjuWuaeZJOazxe4uauvT.6gORpnRsYrEKi7xOCKVZvGeXM6MLaiy', 'demo_admin'),
  (2, NOW(), 0, 1, '$2a$08$urjuWuaeZJOazxe4uauvT.6gORpnRsYrEKi7xOCKVZvGeXM6MLaiy', 'demo_merchant'),
  (3, NOW(), 0, 1, '$2a$08$urjuWuaeZJOazxe4uauvT.6gORpnRsYrEKi7xOCKVZvGeXM6MLaiy', 'demo_user');

INSERT INTO user_authority (user_id, authority_name) VALUES
  (1, 'ADMIN'),
  (1, 'USER'),
  (2, 'BUSINESS'),
  (2, 'USER'),
  (3, 'USER');

INSERT INTO person (id, email, first_name, last_name, gender, phone, photo) VALUES
  (1, 'admin@demo.local', '演示', '管理员', '男', '13800000001', NULL),
  (2, 'merchant@demo.local', '演示', '商家', '女', '13800000002', NULL),
  (3, 'user@demo.local', '演示', '顾客', '女', '13800000003', NULL);

INSERT INTO business
  (id, create_time, is_deleted, business_address, business_explain, business_img,
   business_name, delivery_price, order_type_id, remarks, start_price, user_id, status)
VALUES
  (1, NOW(), 0, '天津大学北洋园校区', '学生最爱的现炒套餐',
   '/images/merchants/01-stir-fry.jpg',
   '北洋食堂·现炒', 2.00, 4, '本地演示数据', 15.00, 2, 1),
  (2, NOW(), 0, '天津大学卫津路校区', '暖心早餐与咖啡',
   '/images/merchants/02-breakfast.jpg',
   '海棠早餐铺', 1.50, 2, '本地演示数据', 10.00, 2, 1),
  (3, NOW(), 0, '天津市和平区', '夜宵炸物与冰饮',
   '/images/merchants/03-chicken-wings.jpg',
   '深夜能量站', 3.00, 10, '本地演示数据', 20.00, 2, 1);

INSERT INTO food
  (id, create_time, is_deleted, food_explain, food_img, food_name, food_price,
   remarks, business_id, shelve_status, category)
VALUES
  (1, NOW(), 0, '鸡腿、时蔬与米饭', NULL, '照烧鸡腿饭', 18.80, NULL, 1, 1, '盖饭'),
  (2, NOW(), 0, '番茄、牛肉与滑蛋', NULL, '番茄滑蛋牛肉饭', 22.00, NULL, 1, 1, '盖饭'),
  (3, NOW(), 0, '清爽低脂套餐', NULL, '香煎鸡胸沙拉', 19.90, NULL, 1, 1, '轻食'),
  (4, NOW(), 0, '现磨咖啡', NULL, '热拿铁', 12.00, NULL, 2, 1, '咖啡饮品'),
  (5, NOW(), 0, '鸡蛋、培根与芝士', NULL, '元气早餐卷', 9.90, NULL, 2, 1, '早餐主食'),
  (6, NOW(), 0, '豆浆与两只包子', NULL, '经典早餐套餐', 8.80, NULL, 2, 1, '早餐套餐'),
  (7, NOW(), 0, '鸡翅、薯条与饮料', NULL, '双人炸物拼盘', 32.00, NULL, 3, 1, '炸物小吃'),
  (8, NOW(), 0, '微辣酥脆', NULL, '香辣鸡排', 15.80, NULL, 3, 1, '炸物小吃'),
  (9, NOW(), 0, '冰爽柠檬气泡水', NULL, '柠檬气泡水', 7.00, NULL, 3, 1, '饮品');

INSERT INTO delivery_address
  (id, create_time, is_deleted, address, contact_name, contact_sex, contact_tel, user_id)
VALUES
  (1, NOW(), 0, '天津大学北洋园校区 45 教 A 区', '演示顾客', 0, '13800000003', 3);

INSERT INTO orders
  (id, create_time, is_deleted, order_date, order_state, order_total,
   business_id, customer_id, address_id, delivery_price)
VALUES
  (1, NOW(), 0, NOW(), 4, 42.80, 1, 3, 1, 2.00),
  (2, NOW(), 0, NOW(), 1, 21.90, 2, 3, 1, 1.50);

INSERT INTO orderdetailet
  (id, create_time, is_deleted, quantity, food_id, order_id, food_price)
VALUES
  (1, NOW(), 0, 1, 1, 1, 18.80),
  (2, NOW(), 0, 1, 2, 1, 22.00),
  (3, NOW(), 0, 1, 4, 2, 12.00),
  (4, NOW(), 0, 1, 5, 2, 9.90);

INSERT INTO merchant_interaction
  (user_id, merchant_id, liked, collected, create_time, update_time)
VALUES
  (3, 1, 1, 1, NOW(), NOW()),
  (3, 2, 1, 0, NOW(), NOW());

-- 顾客资产展示样本：答辩时可直接演示钱包、积分抵扣、会员和多张红包选择。
INSERT INTO user_asset (user_id, balance, points, membership_expire, update_time)
VALUES (3, 100.00, 1200, DATE_ADD(NOW(), INTERVAL 30 DAY), NOW())
ON DUPLICATE KEY UPDATE balance = VALUES(balance), points = VALUES(points),
  membership_expire = VALUES(membership_expire), update_time = NOW();

INSERT INTO user_coupon (user_id, name, discount_amount, min_order_amount, expires_at, used, create_time)
SELECT 3, '校园午餐红包', 3.00, 20.00, DATE_ADD(NOW(), INTERVAL 30 DAY), 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM user_coupon WHERE user_id = 3 AND name = '校园午餐红包');
INSERT INTO user_coupon (user_id, name, discount_amount, min_order_amount, expires_at, used, create_time)
SELECT 3, '满40减6红包', 6.00, 40.00, DATE_ADD(NOW(), INTERVAL 30 DAY), 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM user_coupon WHERE user_id = 3 AND name = '满40减6红包');

-- 首页展示样本：让演示数据同时覆盖高评分、高销量、堂食、满减和新店等场景。
-- demo_* 字段是运营统计快照；没有快照的真实商家仍由订单/互动数据计算。
UPDATE business
SET create_time = DATE_SUB(NOW(), INTERVAL 180 DAY),
    demo_rating = 4.80, demo_sales_count = 1860,
    dine_in_available = 1, promotion_threshold = 20.00, promotion_discount = 3.00
WHERE id = 1;
UPDATE business
SET create_time = DATE_SUB(NOW(), INTERVAL 120 DAY),
    demo_rating = 4.60, demo_sales_count = 920,
    dine_in_available = 0, promotion_threshold = 30.00, promotion_discount = 5.00
WHERE id = 2;
UPDATE business
SET create_time = DATE_SUB(NOW(), INTERVAL 90 DAY),
    demo_rating = 4.30, demo_sales_count = 68,
    dine_in_available = 1, promotion_threshold = NULL, promotion_discount = NULL
WHERE id = 3;

INSERT INTO business
  (id, create_time, is_deleted, business_address, business_explain, business_img,
   business_name, delivery_price, order_type_id, remarks, start_price, user_id, status,
   dine_in_available, promotion_threshold, promotion_discount, demo_rating, demo_sales_count)
VALUES
  (4, DATE_SUB(NOW(), INTERVAL 75 DAY), 0, '天津大学北洋园校区', '现炒锅气足，晚餐和夜宵都受欢迎',
   '/images/merchants/04-hot-pot.jpg',
   '津南麻辣香锅', 2.50, 10, '演示样本·高销量堂食店', 25.00, 2, 1, 1, 50.00, 8.00, 4.80, 860),
  (5, DATE_SUB(NOW(), INTERVAL 60 DAY), 0, '天津大学卫津路校区', '手冲咖啡和低糖甜品',
   '/images/merchants/05-coffee.jpg',
   '北洋咖啡实验室', 1.00, 2, '演示样本·品质咖啡', 20.00, 2, 1, 0, 30.00, 5.00, 4.70, 520),
  (6, DATE_SUB(NOW(), INTERVAL 210 DAY), 0, '天津大学北洋园校区', '汤清面劲道，午餐高峰常排队',
   '/images/merchants/06-beef-noodles.jpg',
   '清真兰州牛肉面', 1.50, 4, '演示样本·月售很高', 15.00, 2, 1, 1, 20.00, 3.00, 4.60, 1280),
  (7, DATE_SUB(NOW(), INTERVAL 45 DAY), 0, '天津大学津南校区', '轻食沙拉和能量碗，适合学习日',
   '/images/merchants/07-salad.jpg',
   '轻食研究所', 2.00, 4, '演示样本·高好评轻食', 25.00, 2, 1, 0, 35.00, 6.00, 4.90, 76),
  (8, DATE_SUB(NOW(), INTERVAL 5 DAY), 0, '天津大学北洋园校区', '刚开业的炸鸡和冰饮小店',
   '/images/merchants/08-chicken-burger.jpg',
   '泰合炸鸡·夜宵', 3.00, 10, '演示样本·新店', 20.00, 2, 1, 0, NULL, NULL, 4.20, 18)
ON DUPLICATE KEY UPDATE
  create_time = VALUES(create_time), is_deleted = 0, business_address = VALUES(business_address),
  business_explain = VALUES(business_explain), business_img = VALUES(business_img),
  business_name = VALUES(business_name), delivery_price = VALUES(delivery_price),
  order_type_id = VALUES(order_type_id), remarks = VALUES(remarks), start_price = VALUES(start_price),
  status = 1, dine_in_available = VALUES(dine_in_available),
  promotion_threshold = VALUES(promotion_threshold), promotion_discount = VALUES(promotion_discount),
  demo_rating = VALUES(demo_rating), demo_sales_count = VALUES(demo_sales_count);

INSERT INTO food
  (id, create_time, is_deleted, food_explain, food_img, food_name, food_price,
   remarks, business_id, shelve_status, stock, category, purchase_limit)
VALUES
  (10, NOW(), 0, '牛肉、土豆和时蔬', NULL, '招牌牛肉麻辣香锅', 32.00, '演示样本', 4, 1, 100, '麻辣香锅', 2),
  (11, NOW(), 0, '鲜虾、玉米和藕片', NULL, '鲜香虾滑锅', 38.00, '演示样本', 4, 1, 100, '麻辣香锅', 2),
  (12, NOW(), 0, '午餐双人套餐', NULL, '双人香锅套餐', 58.00, '演示样本', 4, 1, 100, '套餐', 1),
  (13, NOW(), 0, '低糖冷萃咖啡', NULL, '冰美式', 15.00, '演示样本', 5, 1, 100, '咖啡', 3),
  (14, NOW(), 0, '奶香浓郁', NULL, '生椰拿铁', 19.00, '演示样本', 5, 1, 100, '咖啡', 3),
  (15, NOW(), 0, '巴斯克芝士蛋糕', NULL, '原味巴斯克', 22.00, '演示样本', 5, 1, 100, '甜品', 2),
  (16, NOW(), 0, '手打牛肉丸和香菜', NULL, '经典牛肉面', 18.00, '演示样本', 6, 1, 100, '面食', 3),
  (17, NOW(), 0, '大份牛肉和宽面', NULL, '招牌大碗牛肉面', 25.00, '演示样本', 6, 1, 100, '面食', 2),
  (18, NOW(), 0, '牛肉面和卤蛋', NULL, '学生能量套餐', 24.00, '演示样本', 6, 1, 100, '套餐', 2),
  (19, NOW(), 0, '鸡胸肉、玉米和生菜', NULL, '低脂鸡胸沙拉', 26.00, '演示样本', 7, 1, 100, '轻食', 2),
  (20, NOW(), 0, '牛油果和藜麦', NULL, '牛油果能量碗', 29.00, '演示样本', 7, 1, 100, '轻食', 2),
  (21, NOW(), 0, '酸奶和当季水果', NULL, '水果酸奶杯', 16.00, '演示样本', 7, 1, 100, '甜品', 3),
  (22, NOW(), 0, '原味脆皮鸡', NULL, '脆皮鸡腿堡', 19.90, '演示样本', 8, 1, 100, '汉堡', 3),
  (23, NOW(), 0, '鸡翅和薯条', NULL, '分享炸鸡桶', 36.00, '演示样本', 8, 1, 100, '套餐', 2),
  (24, NOW(), 0, '冰爽柠檬气泡水', NULL, '柠檬气泡水', 8.00, '演示样本', 8, 1, 100, '饮品', 5)
ON DUPLICATE KEY UPDATE
  food_explain = VALUES(food_explain), food_name = VALUES(food_name), food_price = VALUES(food_price),
  business_id = VALUES(business_id), shelve_status = 1, stock = VALUES(stock),
  category = VALUES(category), purchase_limit = VALUES(purchase_limit), is_deleted = 0;

SET FOREIGN_KEY_CHECKS = 1;

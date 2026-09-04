USE elm_v2;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 演示菜单图片全部使用项目内资源，避免答辩现场依赖外网。
UPDATE food SET food_img = CASE id
  WHEN 1 THEN '/images/foods/26-teriyaki-rice.jpg'
  WHEN 2 THEN '/images/foods/35-tomato-eggs.jpg'
  WHEN 3 THEN '/images/foods/01-chicken-salad.jpg'
  WHEN 4 THEN '/images/foods/09-latte.jpg'
  WHEN 5 THEN '/images/foods/05-breakfast.jpg'
  WHEN 6 THEN '/images/foods/20-baozi.jpg'
  WHEN 7 THEN '/images/foods/19-chicken-wings.jpg'
  WHEN 8 THEN '/images/foods/19-chicken-wings.jpg'
  WHEN 9 THEN '/images/foods/14-fruit-drink.jpg'
  WHEN 10 THEN '/images/foods/23-mala-xiang-guo.jpg'
  WHEN 11 THEN '/images/foods/25-malatang-bowl.jpg'
  WHEN 12 THEN '/images/foods/23-mala-xiang-guo.jpg'
  WHEN 13 THEN '/images/foods/08-coffee.jpg'
  WHEN 14 THEN '/images/foods/09-latte.jpg'
  WHEN 15 THEN '/images/foods/11-cake.jpg'
  WHEN 16 THEN '/images/foods/04-noodles.jpg'
  WHEN 17 THEN '/images/foods/04-noodles.jpg'
  WHEN 18 THEN '/images/foods/04-noodles.jpg'
  WHEN 19 THEN '/images/foods/01-chicken-salad.jpg'
  WHEN 20 THEN '/images/foods/06-avocado-toast.jpg'
  WHEN 21 THEN '/images/foods/10-cheesecake.jpg'
  WHEN 22 THEN '/images/foods/12-burger.jpg'
  WHEN 23 THEN '/images/foods/19-chicken-wings.jpg'
  WHEN 24 THEN '/images/foods/14-fruit-drink.jpg'
  ELSE food_img END
WHERE id BETWEEN 1 AND 24;

-- 重点商家采用不同的菜单规模和分类结构，便于展示商家自定义分类能力。
INSERT INTO food
  (id, create_time, is_deleted, food_explain, food_img, food_name, food_price,
   remarks, business_id, shelve_status, stock, category, purchase_limit)
VALUES
  (25, NOW(), 0, '鸡丁鲜嫩，配时蔬与米饭', '/images/foods/28-kung-pao-chicken.jpg', '宫保鸡丁盖饭', 17.80, '演示菜单', 1, 1, 86, '盖饭', 3),
  (26, NOW(), 0, '黑椒牛柳、洋葱与米饭', '/images/foods/29-black-pepper-beef.jpg', '黑椒牛柳饭', 23.80, '演示菜单', 1, 1, 72, '盖饭', 3),
  (27, NOW(), 0, '麻香微辣，下饭经典', '/images/foods/16-mapo-tofu.jpg', '麻婆豆腐', 13.80, '演示菜单', 1, 1, 95, '现炒小菜', 4),
  (28, NOW(), 0, '肉丝、木耳和青笋现炒', '/images/foods/30-yuxiang-pork.jpg', '鱼香肉丝', 16.80, '演示菜单', 1, 1, 68, '现炒小菜', 4),
  (29, NOW(), 0, '鸡蛋、火腿与时蔬粒', '/images/foods/02-fried-rice.jpg', '扬州炒饭', 15.80, '演示菜单', 1, 1, 90, '炒饭', 4),
  (30, NOW(), 0, '鲜虾仁与鸡蛋大火快炒', '/images/foods/31-shrimp-fried-rice.jpg', '虾仁炒饭', 19.80, '演示菜单', 1, 1, 64, '炒饭', 3),
  (31, NOW(), 0, '底脆馅足，六只一份', '/images/foods/32-fried-dumplings.jpg', '鲜肉锅贴', 12.80, '演示菜单', 1, 1, 80, '面点', 4),
  (32, NOW(), 0, '现蒸小笼包，六只一份', '/images/foods/33-xiaolongbao.jpg', '鲜肉小笼包', 11.80, '演示菜单', 1, 1, 78, '面点', 4),
  (33, NOW(), 0, '清爽少糖，现切柠檬', '/images/foods/14-fruit-drink.jpg', '冰柠檬茶', 6.00, '演示菜单', 1, 1, 120, '饮品', 6),
  (34, NOW(), 0, '酸甜解腻，冰镇更佳', '/images/foods/21-plum-drink.jpg', '桂花酸梅汤', 5.00, '演示菜单', 1, 1, 120, '饮品', 6),

  (35, NOW(), 0, '牛肉、午餐肉与六种时蔬', '/images/foods/25-malatang-bowl.jpg', '单人荤素自选锅', 29.80, '演示菜单', 4, 1, 88, '自选香锅', 3),
  (36, NOW(), 0, '豆制品、菌菇与当季鲜蔬', '/images/foods/24-malatang.jpg', '鲜蔬豆制品自选锅', 24.80, '演示菜单', 4, 1, 76, '自选香锅', 3),
  (37, NOW(), 0, '外酥里嫩，椒盐风味', '/images/foods/13-fried-snack.jpg', '手打牛肉丸', 15.80, '演示菜单', 4, 1, 62, '特色小吃', 3),
  (38, NOW(), 0, '金黄酥脆，香辣入味', '/images/foods/19-chicken-wings.jpg', '香辣鸡翅', 16.80, '演示菜单', 4, 1, 70, '特色小吃', 3),
  (39, NOW(), 0, '红糖风味，软糯香甜', '/images/foods/07-pancake.jpg', '红糖糍粑', 12.00, '演示菜单', 4, 1, 66, '特色小吃', 3),
  (40, NOW(), 0, '冰凉清甜，搭配葡萄干', '/images/foods/14-fruit-drink.jpg', '手工冰粉', 8.00, '演示菜单', 4, 1, 100, '饮品', 5),
  (41, NOW(), 0, '酸甜解辣，冰镇桂花香', '/images/foods/21-plum-drink.jpg', '桂花酸梅汤', 6.00, '演示菜单', 4, 1, 100, '饮品', 5),
  (42, NOW(), 0, '一荤五素，含米饭和饮品', '/images/foods/23-mala-xiang-guo.jpg', '单人超值香锅套餐', 36.80, '演示菜单', 4, 1, 58, '套餐', 2),

  (43, NOW(), 0, '现蒸鲜肉包，三只一份', '/images/foods/33-xiaolongbao.jpg', '鲜肉小笼包', 8.80, '演示菜单', 2, 1, 100, '中式早点', 5),
  (44, NOW(), 0, '底脆馅香，四只一份', '/images/foods/32-fried-dumplings.jpg', '鲜肉锅贴', 9.80, '演示菜单', 2, 1, 90, '中式早点', 5),
  (45, NOW(), 0, '太阳蛋、牛油果与全麦吐司', '/images/foods/05-breakfast.jpg', '太阳蛋吐司', 12.80, '演示菜单', 2, 1, 72, '西式早餐', 3),
  (46, NOW(), 0, '牛油果、水煮蛋和生菜', '/images/foods/06-avocado-toast.jpg', '牛油果鸡蛋吐司', 15.80, '演示菜单', 2, 1, 68, '西式早餐', 3),
  (47, NOW(), 0, '燕麦奶融合浓缩咖啡', '/images/foods/09-latte.jpg', '燕麦拿铁', 14.00, '演示菜单', 2, 1, 100, '咖啡饮品', 4),
  (48, NOW(), 0, '冰爽气泡与鲜切柠檬', '/images/foods/14-fruit-drink.jpg', '冰柠檬气泡水', 8.00, '演示菜单', 2, 1, 110, '清爽饮品', 5),
  (49, NOW(), 0, '双人主食、咖啡和水果', '/images/foods/05-breakfast.jpg', '双人早餐组合', 29.80, '演示菜单', 2, 1, 50, '早餐套餐', 2),

  (50, NOW(), 0, '焦糖香气与绵密奶泡', '/images/foods/08-coffee.jpg', '焦糖玛奇朵', 20.00, '演示菜单', 5, 1, 92, '咖啡', 3),
  (51, NOW(), 0, '柑橘花香，明亮酸质', '/images/foods/09-latte.jpg', '手冲耶加雪菲', 26.00, '演示菜单', 5, 1, 66, '咖啡', 3),
  (52, NOW(), 0, '草莓奶油与松软蛋糕胚', '/images/foods/10-cheesecake.jpg', '草莓奶油杯', 18.00, '演示菜单', 5, 1, 58, '甜品', 3),
  (53, NOW(), 0, '浓郁可可，口感湿润', '/images/foods/11-cake.jpg', '巧克力蛋糕', 23.00, '演示菜单', 5, 1, 54, '甜品', 2),
  (54, NOW(), 0, '蓝莓、香蕉与蜂蜜松饼', '/images/foods/07-pancake.jpg', '蓝莓松饼', 19.00, '演示菜单', 5, 1, 62, '甜品', 2),
  (55, NOW(), 0, '牛油果、鸡蛋与全麦吐司', '/images/foods/06-avocado-toast.jpg', '牛油果开放三明治', 22.00, '演示菜单', 5, 1, 70, '轻食', 2),
  (56, NOW(), 0, '烤鸡胸、谷物和新鲜时蔬', '/images/foods/01-chicken-salad.jpg', '烤鸡轻食碗', 25.00, '演示菜单', 5, 1, 65, '轻食', 2),
  (57, NOW(), 0, '伯爵茶香与牛乳融合', '/images/foods/15-tea.jpg', '伯爵鲜奶茶', 16.00, '演示菜单', 5, 1, 88, '茶饮', 4)
ON DUPLICATE KEY UPDATE
  food_explain = VALUES(food_explain), food_img = VALUES(food_img), food_name = VALUES(food_name),
  food_price = VALUES(food_price), business_id = VALUES(business_id), shelve_status = 1,
  stock = VALUES(stock), category = VALUES(category), purchase_limit = VALUES(purchase_limit), is_deleted = 0;

-- 修正早期样本的分类，使同一家店的分类命名保持一致。
UPDATE food SET category = '西式早餐' WHERE id = 5;

-- 评价展示账号仅用于历史数据，不在演示账号列表中公开。
INSERT INTO users (id, create_time, is_deleted, activated, password, username) VALUES
  (11, DATE_SUB(NOW(), INTERVAL 90 DAY), 0, 1, '$2a$08$urjuWuaeZJOazxe4uauvT.6gORpnRsYrEKi7xOCKVZvGeXM6MLaiy', '天津干饭人'),
  (12, DATE_SUB(NOW(), INTERVAL 80 DAY), 0, 1, '$2a$08$urjuWuaeZJOazxe4uauvT.6gORpnRsYrEKi7xOCKVZvGeXM6MLaiy', '海棠小饭团'),
  (13, DATE_SUB(NOW(), INTERVAL 70 DAY), 0, 1, '$2a$08$urjuWuaeZJOazxe4uauvT.6gORpnRsYrEKi7xOCKVZvGeXM6MLaiy', '卫津路同学'),
  (14, DATE_SUB(NOW(), INTERVAL 60 DAY), 0, 1, '$2a$08$urjuWuaeZJOazxe4uauvT.6gORpnRsYrEKi7xOCKVZvGeXM6MLaiy', '今晚吃什么'),
  (15, DATE_SUB(NOW(), INTERVAL 50 DAY), 0, 1, '$2a$08$urjuWuaeZJOazxe4uauvT.6gORpnRsYrEKi7xOCKVZvGeXM6MLaiy', '北洋园学子'),
  (16, DATE_SUB(NOW(), INTERVAL 40 DAY), 0, 1, '$2a$08$urjuWuaeZJOazxe4uauvT.6gORpnRsYrEKi7xOCKVZvGeXM6MLaiy', '咖啡续命中'),
  (17, DATE_SUB(NOW(), INTERVAL 30 DAY), 0, 1, '$2a$08$urjuWuaeZJOazxe4uauvT.6gORpnRsYrEKi7xOCKVZvGeXM6MLaiy', '轻食打卡员'),
  (18, DATE_SUB(NOW(), INTERVAL 20 DAY), 0, 1, '$2a$08$urjuWuaeZJOazxe4uauvT.6gORpnRsYrEKi7xOCKVZvGeXM6MLaiy', '夜宵研究生')
ON DUPLICATE KEY UPDATE username = VALUES(username), activated = 1, is_deleted = 0;

INSERT IGNORE INTO user_authority (user_id, authority_name) VALUES
  (11, 'USER'), (12, 'USER'), (13, 'USER'), (14, 'USER'),
  (15, 'USER'), (16, 'USER'), (17, 'USER'), (18, 'USER');

-- 每条评价都关联一笔已完成订单，保持评价业务约束真实有效。
INSERT INTO orders
  (id, create_time, is_deleted, order_date, order_state, order_total, business_id,
   customer_id, address_id, delivery_price, payment_method, payment_status, service_mode)
VALUES
  (1001, DATE_SUB(NOW(), INTERVAL 3 DAY), 0, DATE_SUB(NOW(), INTERVAL 3 DAY), 7, 20.80, 1, 11, NULL, 2.00, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1002, DATE_SUB(NOW(), INTERVAL 9 DAY), 0, DATE_SUB(NOW(), INTERVAL 9 DAY), 7, 24.00, 1, 12, NULL, 2.00, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1003, DATE_SUB(NOW(), INTERVAL 18 DAY), 0, DATE_SUB(NOW(), INTERVAL 18 DAY), 7, 23.80, 1, 14, NULL, 0.00, 'SIMULATED', 'PAID', 'PICKUP'),
  (1004, DATE_SUB(NOW(), INTERVAL 31 DAY), 0, DATE_SUB(NOW(), INTERVAL 31 DAY), 7, 37.60, 1, 15, NULL, 2.00, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1005, DATE_SUB(NOW(), INTERVAL 2 DAY), 0, DATE_SUB(NOW(), INTERVAL 2 DAY), 7, 15.50, 2, 13, NULL, 1.50, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1006, DATE_SUB(NOW(), INTERVAL 11 DAY), 0, DATE_SUB(NOW(), INTERVAL 11 DAY), 7, 23.40, 2, 16, NULL, 1.50, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1007, DATE_SUB(NOW(), INTERVAL 20 DAY), 0, DATE_SUB(NOW(), INTERVAL 20 DAY), 7, 29.80, 2, 12, NULL, 0.00, 'SIMULATED', 'PAID', 'PICKUP'),
  (1008, DATE_SUB(NOW(), INTERVAL 34 DAY), 0, DATE_SUB(NOW(), INTERVAL 34 DAY), 7, 14.30, 2, 11, NULL, 1.50, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1009, DATE_SUB(NOW(), INTERVAL 4 DAY), 0, DATE_SUB(NOW(), INTERVAL 4 DAY), 7, 35.00, 3, 18, NULL, 3.00, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1010, DATE_SUB(NOW(), INTERVAL 12 DAY), 0, DATE_SUB(NOW(), INTERVAL 12 DAY), 7, 15.80, 3, 14, NULL, 0.00, 'SIMULATED', 'PAID', 'PICKUP'),
  (1011, DATE_SUB(NOW(), INTERVAL 24 DAY), 0, DATE_SUB(NOW(), INTERVAL 24 DAY), 7, 42.00, 3, 15, NULL, 3.00, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1012, DATE_SUB(NOW(), INTERVAL 42 DAY), 0, DATE_SUB(NOW(), INTERVAL 42 DAY), 7, 25.80, 3, 13, NULL, 3.00, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1013, DATE_SUB(NOW(), INTERVAL 1 DAY), 0, DATE_SUB(NOW(), INTERVAL 1 DAY), 7, 38.00, 4, 15, NULL, 0.00, 'SIMULATED', 'PAID', 'PICKUP'),
  (1014, DATE_SUB(NOW(), INTERVAL 8 DAY), 0, DATE_SUB(NOW(), INTERVAL 8 DAY), 7, 40.50, 4, 18, NULL, 2.50, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1015, DATE_SUB(NOW(), INTERVAL 16 DAY), 0, DATE_SUB(NOW(), INTERVAL 16 DAY), 7, 60.50, 4, 11, NULL, 2.50, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1016, DATE_SUB(NOW(), INTERVAL 29 DAY), 0, DATE_SUB(NOW(), INTERVAL 29 DAY), 7, 36.80, 4, 12, NULL, 0.00, 'SIMULATED', 'PAID', 'PICKUP'),
  (1017, DATE_SUB(NOW(), INTERVAL 5 DAY), 0, DATE_SUB(NOW(), INTERVAL 5 DAY), 7, 23.00, 5, 16, NULL, 1.00, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1018, DATE_SUB(NOW(), INTERVAL 13 DAY), 0, DATE_SUB(NOW(), INTERVAL 13 DAY), 7, 39.00, 5, 17, NULL, 1.00, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1019, DATE_SUB(NOW(), INTERVAL 22 DAY), 0, DATE_SUB(NOW(), INTERVAL 22 DAY), 7, 26.00, 5, 13, NULL, 0.00, 'SIMULATED', 'PAID', 'PICKUP'),
  (1020, DATE_SUB(NOW(), INTERVAL 38 DAY), 0, DATE_SUB(NOW(), INTERVAL 38 DAY), 7, 46.00, 5, 14, NULL, 1.00, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1021, DATE_SUB(NOW(), INTERVAL 6 DAY), 0, DATE_SUB(NOW(), INTERVAL 6 DAY), 7, 19.50, 6, 11, NULL, 1.50, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1022, DATE_SUB(NOW(), INTERVAL 14 DAY), 0, DATE_SUB(NOW(), INTERVAL 14 DAY), 7, 26.50, 6, 15, NULL, 1.50, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1023, DATE_SUB(NOW(), INTERVAL 26 DAY), 0, DATE_SUB(NOW(), INTERVAL 26 DAY), 7, 24.00, 6, 18, NULL, 0.00, 'SIMULATED', 'PAID', 'PICKUP'),
  (1024, DATE_SUB(NOW(), INTERVAL 45 DAY), 0, DATE_SUB(NOW(), INTERVAL 45 DAY), 7, 37.50, 6, 12, NULL, 1.50, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1025, DATE_SUB(NOW(), INTERVAL 3 DAY), 0, DATE_SUB(NOW(), INTERVAL 3 DAY), 7, 28.00, 7, 17, NULL, 2.00, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1026, DATE_SUB(NOW(), INTERVAL 10 DAY), 0, DATE_SUB(NOW(), INTERVAL 10 DAY), 7, 31.00, 7, 16, NULL, 2.00, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1027, DATE_SUB(NOW(), INTERVAL 19 DAY), 0, DATE_SUB(NOW(), INTERVAL 19 DAY), 7, 26.00, 7, 13, NULL, 0.00, 'SIMULATED', 'PAID', 'PICKUP'),
  (1028, DATE_SUB(NOW(), INTERVAL 33 DAY), 0, DATE_SUB(NOW(), INTERVAL 33 DAY), 7, 47.00, 7, 14, NULL, 2.00, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1029, DATE_SUB(NOW(), INTERVAL 2 DAY), 0, DATE_SUB(NOW(), INTERVAL 2 DAY), 7, 22.90, 8, 18, NULL, 3.00, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1030, DATE_SUB(NOW(), INTERVAL 7 DAY), 0, DATE_SUB(NOW(), INTERVAL 7 DAY), 7, 39.00, 8, 12, NULL, 3.00, 'SIMULATED', 'PAID', 'DELIVERY'),
  (1031, DATE_SUB(NOW(), INTERVAL 15 DAY), 0, DATE_SUB(NOW(), INTERVAL 15 DAY), 7, 36.00, 8, 15, NULL, 0.00, 'SIMULATED', 'PAID', 'PICKUP'),
  (1032, DATE_SUB(NOW(), INTERVAL 28 DAY), 0, DATE_SUB(NOW(), INTERVAL 28 DAY), 7, 30.90, 8, 11, NULL, 3.00, 'SIMULATED', 'PAID', 'DELIVERY')
ON DUPLICATE KEY UPDATE order_state = 7, payment_status = 'PAID', order_total = VALUES(order_total);

INSERT INTO orderdetailet
  (id, create_time, is_deleted, quantity, food_id, order_id, food_price)
VALUES
  (2001,NOW(),0,1,1,1001,18.80),
  (2002,NOW(),0,1,2,1002,22.00),
  (2003,NOW(),0,1,26,1003,23.80),
  (2004,NOW(),0,2,25,1004,17.80),
  (2005,NOW(),0,1,47,1005,14.00),
  (2006,NOW(),0,1,4,1006,12.00),
  (2007,NOW(),0,1,5,1006,9.90),
  (2008,NOW(),0,1,49,1007,29.80),
  (2009,NOW(),0,1,45,1008,12.80),
  (2010,NOW(),0,1,7,1009,32.00),
  (2011,NOW(),0,1,8,1010,15.80),
  (2012,NOW(),0,1,7,1011,32.00),
  (2013,NOW(),0,1,9,1011,7.00),
  (2014,NOW(),0,1,8,1012,15.80),
  (2015,NOW(),0,1,9,1012,7.00),
  (2016,NOW(),0,1,11,1013,38.00),
  (2017,NOW(),0,1,10,1014,32.00),
  (2018,NOW(),0,1,41,1014,6.00),
  (2019,NOW(),0,1,12,1015,58.00),
  (2020,NOW(),0,1,42,1016,36.80),
  (2021,NOW(),0,1,15,1017,22.00),
  (2022,NOW(),0,1,50,1018,20.00),
  (2023,NOW(),0,1,52,1018,18.00),
  (2024,NOW(),0,1,51,1019,26.00),
  (2025,NOW(),0,1,53,1020,23.00),
  (2026,NOW(),0,1,55,1020,22.00),
  (2027,NOW(),0,1,16,1021,18.00),
  (2028,NOW(),0,1,17,1022,25.00),
  (2029,NOW(),0,1,18,1023,24.00),
  (2030,NOW(),0,2,16,1024,18.00),
  (2031,NOW(),0,1,19,1025,26.00),
  (2032,NOW(),0,1,20,1026,29.00),
  (2033,NOW(),0,1,19,1027,26.00),
  (2034,NOW(),0,1,20,1028,29.00),
  (2035,NOW(),0,1,21,1028,16.00),
  (2036,NOW(),0,1,22,1029,19.90),
  (2037,NOW(),0,1,23,1030,36.00),
  (2038,NOW(),0,1,23,1031,36.00),
  (2039,NOW(),0,1,22,1032,19.90),
  (2040,NOW(),0,1,24,1032,8.00)
ON DUPLICATE KEY UPDATE
  quantity = VALUES(quantity), food_id = VALUES(food_id), order_id = VALUES(order_id),
  food_price = VALUES(food_price), is_deleted = 0;

INSERT INTO review
  (id, order_id, customer_id, business_id, rating, content, images, merchant_reply,
   reply_time, create_time, update_time, is_hidden)
VALUES
  (101,1001,11,1,5,'照烧鸡腿饭分量很足，送到教学楼还是热的。',NULL,'谢谢认可，我们会继续保证出餐速度。',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),NOW(),0),
  (102,1002,12,1,5,'现炒味道很香，番茄滑蛋牛肉饭很下饭。',NULL,NULL,NULL,DATE_SUB(NOW(),INTERVAL 9 DAY),NOW(),0),
  (103,1003,14,1,4,'自取很方便，高峰期等了几分钟，但口味不错。',NULL,'高峰期我们会继续优化备餐安排。',DATE_SUB(NOW(),INTERVAL 17 DAY),DATE_SUB(NOW(),INTERVAL 18 DAY),NOW(),0),
  (104,1004,15,1,5,'价格适合学生，饮品和盖饭一起点很划算。',NULL,NULL,NULL,DATE_SUB(NOW(),INTERVAL 31 DAY),NOW(),0),
  (105,1005,13,2,5,'小笼包是现蒸的，拿铁也没有太甜。',NULL,'感谢喜欢，早餐时段欢迎再来。',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),NOW(),0),
  (106,1006,16,2,4,'咖啡香气不错，早餐卷如果再热一点更好。',NULL,'收到建议，我们会注意保温。',DATE_SUB(NOW(),INTERVAL 10 DAY),DATE_SUB(NOW(),INTERVAL 11 DAY),NOW(),0),
  (107,1007,12,2,5,'双人早餐搭配很完整，自取不用等太久。',NULL,NULL,NULL,DATE_SUB(NOW(),INTERVAL 20 DAY),NOW(),0),
  (108,1008,11,2,4,'分量刚好，包装干净，适合早八前带走。',NULL,NULL,NULL,DATE_SUB(NOW(),INTERVAL 34 DAY),NOW(),0),
  (109,1009,18,3,4,'鸡翅外皮酥脆，夜里下单也很快。',NULL,'感谢支持，夜宵时段正常营业。',DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY),NOW(),0),
  (110,1010,14,3,4,'鸡排微辣刚好，气泡水很解腻。',NULL,NULL,NULL,DATE_SUB(NOW(),INTERVAL 12 DAY),NOW(),0),
  (111,1011,15,3,5,'双人拼盘量很实在，宿舍一起吃很合适。',NULL,'谢谢推荐，拼盘会继续保持分量。',DATE_SUB(NOW(),INTERVAL 23 DAY),DATE_SUB(NOW(),INTERVAL 24 DAY),NOW(),0),
  (112,1012,13,3,4,'味道不错，薯条送到后稍微软了一点。',NULL,'抱歉影响口感，我们会改进透气包装。',DATE_SUB(NOW(),INTERVAL 41 DAY),DATE_SUB(NOW(),INTERVAL 42 DAY),NOW(),0),
  (113,1013,15,4,5,'香锅很入味，分类清楚，选套餐也很方便。',NULL,'谢谢喜欢，辣度也可以备注调整。',NOW(),DATE_SUB(NOW(),INTERVAL 1 DAY),NOW(),0),
  (114,1014,18,4,5,'牛肉和配菜都不少，酸梅汤特别解辣。',NULL,NULL,NULL,DATE_SUB(NOW(),INTERVAL 8 DAY),NOW(),0),
  (115,1015,11,4,4,'口味不错，高峰期配送稍慢，但没有洒漏。',NULL,'感谢理解，我们会和骑手一起优化高峰配送。',DATE_SUB(NOW(),INTERVAL 15 DAY),DATE_SUB(NOW(),INTERVAL 16 DAY),NOW(),0),
  (116,1016,12,4,5,'自取套餐性价比高，小酥肉也很香。',NULL,NULL,NULL,DATE_SUB(NOW(),INTERVAL 29 DAY),NOW(),0),
  (117,1017,16,5,5,'咖啡豆风味很干净，包装也很有质感。',NULL,'感谢认可，豆单会定期更新。',DATE_SUB(NOW(),INTERVAL 4 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),NOW(),0),
  (118,1018,17,5,5,'轻食碗蔬菜新鲜，配拿铁很适合下午学习。',NULL,NULL,NULL,DATE_SUB(NOW(),INTERVAL 13 DAY),NOW(),0),
  (119,1019,13,5,4,'蛋糕口感不错，希望以后增加无糖选项。',NULL,'已经记录，会评估增加低糖甜品。',DATE_SUB(NOW(),INTERVAL 21 DAY),DATE_SUB(NOW(),INTERVAL 22 DAY),NOW(),0),
  (120,1020,14,5,5,'手冲和甜品都在线，店铺故事也很有意思。',NULL,NULL,NULL,DATE_SUB(NOW(),INTERVAL 38 DAY),NOW(),0),
  (121,1021,11,6,5,'牛肉面汤很香，面条也有嚼劲。',NULL,'谢谢认可，面条都是现拉现煮。',DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY),NOW(),0),
  (122,1022,15,6,4,'大碗确实够吃，午餐时间出餐稍慢。',NULL,'高峰期已增加备餐人员，感谢建议。',DATE_SUB(NOW(),INTERVAL 13 DAY),DATE_SUB(NOW(),INTERVAL 14 DAY),NOW(),0),
  (123,1023,18,6,5,'自取速度快，牛肉给得比预想中多。',NULL,NULL,NULL,DATE_SUB(NOW(),INTERVAL 26 DAY),NOW(),0),
  (124,1024,12,6,4,'味道很稳，卤蛋如果更入味就更好了。',NULL,NULL,NULL,DATE_SUB(NOW(),INTERVAL 45 DAY),NOW(),0),
  (125,1025,17,7,5,'鸡胸肉不柴，蔬菜和玉米都很新鲜。',NULL,'感谢打卡，我们坚持当天备菜。',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),NOW(),0),
  (126,1026,16,7,5,'能量碗搭配均衡，酱汁可以分开放很贴心。',NULL,NULL,NULL,DATE_SUB(NOW(),INTERVAL 10 DAY),NOW(),0),
  (127,1027,13,7,5,'控制热量时很适合点，分量也不会太少。',NULL,'谢谢喜欢，轻食也要吃得满足。',DATE_SUB(NOW(),INTERVAL 18 DAY),DATE_SUB(NOW(),INTERVAL 19 DAY),NOW(),0),
  (128,1028,14,7,4,'味道清爽，水果酸奶杯稍微有点甜。',NULL,'可以备注少甜，我们会按需求制作。',DATE_SUB(NOW(),INTERVAL 32 DAY),DATE_SUB(NOW(),INTERVAL 33 DAY),NOW(),0),
  (129,1029,18,8,4,'新店出餐挺快，鸡腿堡外皮很脆。',NULL,'感谢第一批顾客的支持。',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),NOW(),0),
  (130,1030,12,8,5,'炸鸡桶适合宿舍分享，分量很足。',NULL,NULL,NULL,DATE_SUB(NOW(),INTERVAL 7 DAY),NOW(),0),
  (131,1031,15,8,4,'自取方便，店员核对订单很认真。',NULL,'谢谢认可，欢迎再次光临。',DATE_SUB(NOW(),INTERVAL 14 DAY),DATE_SUB(NOW(),INTERVAL 15 DAY),NOW(),0),
  (132,1032,11,8,4,'整体不错，柠檬气泡水搭炸鸡很合适。',NULL,NULL,NULL,DATE_SUB(NOW(),INTERVAL 28 DAY),NOW(),0)
ON DUPLICATE KEY UPDATE
  rating = VALUES(rating), content = VALUES(content), merchant_reply = VALUES(merchant_reply),
  reply_time = VALUES(reply_time), create_time = VALUES(create_time), is_hidden = 0;

SET FOREIGN_KEY_CHECKS = 1;

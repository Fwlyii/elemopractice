USE elm_v2;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

INSERT IGNORE INTO authority (`name`) VALUES ('RIDER');

INSERT INTO users
  (id, create_time, is_deleted, activated, password, username)
VALUES
  (4, NOW(), 0, 1, '$2a$08$urjuWuaeZJOazxe4uauvT.6gORpnRsYrEKi7xOCKVZvGeXM6MLaiy', 'demo_rider'),
  (5, NOW(), 0, 1, '$2a$08$urjuWuaeZJOazxe4uauvT.6gORpnRsYrEKi7xOCKVZvGeXM6MLaiy', 'demo_rider_candidate')
ON DUPLICATE KEY UPDATE username = VALUES(username);

INSERT IGNORE INTO user_authority (user_id, authority_name) VALUES
  (4, 'RIDER'),
  (4, 'USER'),
  (5, 'USER');

INSERT INTO person (id, email, first_name, last_name, gender, phone, photo) VALUES
  (4, 'rider@demo.local', '海棠', '骑手', '男', '13800000004', NULL),
  (5, 'candidate@demo.local', '新人', '骑手', '女', '13800000005', NULL)
ON DUPLICATE KEY UPDATE phone = VALUES(phone), email = VALUES(email);

INSERT INTO rider_profile
  (id, user_id, real_name, phone, vehicle_type, audit_status, online,
   completed_orders, total_distance, total_income, create_time, update_time)
VALUES
  (1, 4, '林海', '13800000004', 'E_BIKE', 1, 1, 12, 28.60, 36.00, NOW(), NOW()),
  (2, 5, '周雨', '13800000005', 'BIKE', 0, 0, 0, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  real_name = VALUES(real_name), phone = VALUES(phone), vehicle_type = VALUES(vehicle_type);

UPDATE orders SET order_state = 7, update_time = NOW() WHERE id = 1;

INSERT INTO orders
  (id, create_time, is_deleted, order_date, order_state, order_total,
   business_id, customer_id, address_id, delivery_price)
VALUES
  (3, NOW(), 0, NOW(), 3, 41.60, 3, 3, 1, 3.00)
ON DUPLICATE KEY UPDATE order_state = VALUES(order_state), update_time = NOW();

INSERT IGNORE INTO orderdetailet
  (id, create_time, is_deleted, quantity, food_id, order_id, food_price)
VALUES
  (5, NOW(), 0, 1, 7, 3, 32.00),
  (6, NOW(), 0, 1, 9, 3, 7.00);

INSERT INTO delivery_task
  (id, order_id, rider_user_id, task_status, version, distance_km, rider_fee,
   accepted_time, arrived_store_time, pickup_time, delivered_time, completed_time,
   create_time, update_time)
VALUES
  (1, 1, 4, 'COMPLETED', 5, 2.80, 2.00,
   DATE_SUB(NOW(), INTERVAL 38 MINUTE), DATE_SUB(NOW(), INTERVAL 31 MINUTE),
   DATE_SUB(NOW(), INTERVAL 28 MINUTE), DATE_SUB(NOW(), INTERVAL 5 MINUTE),
   DATE_SUB(NOW(), INTERVAL 2 MINUTE), DATE_SUB(NOW(), INTERVAL 40 MINUTE), NOW()),
  (2, 3, NULL, 'WAITING_RIDER', 0, 3.60, 3.00,
   NULL, NULL, NULL, NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  rider_user_id = VALUES(rider_user_id), task_status = VALUES(task_status),
  distance_km = VALUES(distance_km), rider_fee = VALUES(rider_fee), update_time = NOW();

INSERT INTO order_status_history
  (order_id, from_status, to_status, operator_user_id, reason, create_time)
SELECT 3, 1, 2, 2, '商家接单，开始创建配送任务', DATE_SUB(NOW(), INTERVAL 2 MINUTE)
WHERE NOT EXISTS (
  SELECT 1 FROM order_status_history WHERE order_id = 3 AND to_status = 2
);

INSERT INTO order_status_history
  (order_id, from_status, to_status, operator_user_id, reason, create_time)
SELECT 3, 2, 3, 2, '配送任务已进入骑手任务大厅', DATE_SUB(NOW(), INTERVAL 1 MINUTE)
WHERE NOT EXISTS (
  SELECT 1 FROM order_status_history WHERE order_id = 3 AND to_status = 3
);

SET FOREIGN_KEY_CHECKS = 1;

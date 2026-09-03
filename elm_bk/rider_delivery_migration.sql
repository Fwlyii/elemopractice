USE elm_v2;

INSERT IGNORE INTO authority (`name`) VALUES ('RIDER');

CREATE TABLE IF NOT EXISTS rider_profile (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  real_name varchar(50) NOT NULL,
  phone varchar(20) NOT NULL,
  vehicle_type varchar(20) NOT NULL,
  audit_status tinyint NOT NULL DEFAULT 0 COMMENT '0-待审核 1-已通过 2-已拒绝',
  online tinyint(1) NOT NULL DEFAULT 0,
  reject_reason varchar(255) NULL,
  completed_orders int NOT NULL DEFAULT 0,
  total_distance decimal(10,2) NOT NULL DEFAULT 0,
  total_income decimal(10,2) NOT NULL DEFAULT 0,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rider_user (user_id),
  INDEX idx_rider_audit_online (audit_status, online),
  CONSTRAINT fk_rider_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='骑手档案与资质审核';

CREATE TABLE IF NOT EXISTS delivery_task (
  id bigint NOT NULL AUTO_INCREMENT,
  order_id bigint NOT NULL,
  rider_user_id bigint NULL,
  task_status varchar(32) NOT NULL,
  version int NOT NULL DEFAULT 0,
  distance_km decimal(10,2) NOT NULL DEFAULT 0,
  rider_fee decimal(10,2) NOT NULL DEFAULT 0,
  accepted_time datetime NULL,
  arrived_store_time datetime NULL,
  pickup_time datetime NULL,
  delivered_time datetime NULL,
  completed_time datetime NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_delivery_order (order_id),
  INDEX idx_delivery_pool (task_status, rider_user_id, create_time),
  INDEX idx_delivery_rider (rider_user_id, task_status),
  CONSTRAINT fk_delivery_order FOREIGN KEY (order_id) REFERENCES orders (id),
  CONSTRAINT fk_delivery_rider FOREIGN KEY (rider_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送任务';

CREATE TABLE IF NOT EXISTS delivery_exception (
  id bigint NOT NULL AUTO_INCREMENT,
  task_id bigint NOT NULL,
  rider_user_id bigint NOT NULL,
  exception_type varchar(40) NOT NULL,
  description varchar(500) NOT NULL,
  previous_task_status varchar(32) NOT NULL,
  status tinyint NOT NULL DEFAULT 0,
  resolution_action varchar(20) NULL,
  resolution_note varchar(500) NULL,
  resolver_user_id bigint NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_time datetime NULL,
  PRIMARY KEY (id),
  INDEX idx_exception_status (status, create_time),
  CONSTRAINT fk_exception_task FOREIGN KEY (task_id) REFERENCES delivery_task (id),
  CONSTRAINT fk_exception_rider FOREIGN KEY (rider_user_id) REFERENCES users (id),
  CONSTRAINT fk_exception_resolver FOREIGN KEY (resolver_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送异常工单';

CREATE TABLE IF NOT EXISTS order_status_history (
  id bigint NOT NULL AUTO_INCREMENT,
  order_id bigint NOT NULL,
  from_status int NULL,
  to_status int NOT NULL,
  operator_user_id bigint NULL,
  reason varchar(255) NOT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_order_history (order_id, create_time),
  CONSTRAINT fk_history_order FOREIGN KEY (order_id) REFERENCES orders (id),
  CONSTRAINT fk_history_operator FOREIGN KEY (operator_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单履约状态轨迹';

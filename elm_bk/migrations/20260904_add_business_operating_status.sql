-- 已有数据库升级脚本：将“审核状态”和“营业/休息状态”拆开。
ALTER TABLE business
  ADD COLUMN operating_status TINYINT(1) NOT NULL DEFAULT 1
  COMMENT '营业状态：1营业中，0休息中'
  AFTER status;

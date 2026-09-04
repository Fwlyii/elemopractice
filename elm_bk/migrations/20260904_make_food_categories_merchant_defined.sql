-- 将旧版营销标签式分类收敛为普通菜品分类；以后分类名称由商家维护。
UPDATE food
SET category = '其他'
WHERE category IS NULL
   OR TRIM(category) = ''
   OR category IN ('招牌推荐', '热销菜品');

ALTER TABLE food
  MODIFY COLUMN category VARCHAR(32) NOT NULL DEFAULT '其他'
  COMMENT '商家自定义商品分类';

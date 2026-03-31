-- 已有库增量：为 user_coupon 增加用券关联字段（订单号、使用时间）
ALTER TABLE `user_coupon`
    ADD COLUMN `order_id` BIGINT NULL COMMENT '使用时关联的订单ID' AFTER `expired_at`,
    ADD COLUMN `use_time` DATETIME NULL COMMENT '使用时间' AFTER `order_id`;

CREATE INDEX `idx_order_id` ON `user_coupon` (`order_id`);


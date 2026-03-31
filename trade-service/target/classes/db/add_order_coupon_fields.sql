-- 已有库增量：为订单表增加优惠券与金额字段
-- 注意：表名为 `order`，是 MySQL 关键字，需使用反引号
ALTER TABLE `order`
    ADD COLUMN `coupon_id` BIGINT NULL COMMENT '使用的优惠券ID' AFTER `total_fee`,
    ADD COLUMN `discount_fee` INT NOT NULL DEFAULT 0 COMMENT '优惠金额（分）' AFTER `coupon_id`,
    ADD COLUMN `pay_fee` INT NOT NULL DEFAULT 0 COMMENT '应付金额（分）' AFTER `discount_fee`;

CREATE INDEX `idx_coupon_id` ON `order` (`coupon_id`);


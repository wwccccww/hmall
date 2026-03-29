-- 已有库增量：为 coupon 表增加创建者
ALTER TABLE `coupon`
    ADD COLUMN `creator_id` BIGINT NULL COMMENT '创建者用户ID（管理员）' AFTER `status`;

CREATE INDEX `idx_creator_id` ON `coupon` (`creator_id`);

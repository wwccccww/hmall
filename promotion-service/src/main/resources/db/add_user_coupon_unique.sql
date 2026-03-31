-- 已有库增量：为 user_coupon 增加“同一用户同一券只能领一次”的唯一约束
ALTER TABLE `user_coupon`
    ADD UNIQUE KEY `uk_user_coupon` (`user_id`, `coupon_id`);


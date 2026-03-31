-- 新增：优惠券适用品牌/商家范围关联表（按需执行）

CREATE TABLE IF NOT EXISTS `coupon_brand` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `coupon_id` BIGINT NOT NULL COMMENT '优惠券ID',
    `brand_name` VARCHAR(100) NOT NULL COMMENT '品牌名称（与 item.brand 一致）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_coupon_brand` (`coupon_id`, `brand_name`),
    KEY `idx_coupon_id` (`coupon_id`),
    KEY `idx_brand_name` (`brand_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券适用品牌（按品牌名称匹配）';

CREATE TABLE IF NOT EXISTS `coupon_shop` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `coupon_id` BIGINT NOT NULL COMMENT '优惠券ID',
    `shop_id` BIGINT NOT NULL COMMENT '商家ID（需要业务侧提供）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_coupon_shop` (`coupon_id`, `shop_id`),
    KEY `idx_coupon_id` (`coupon_id`),
    KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券适用商家（按商家ID匹配）';


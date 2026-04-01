-- 促销服务初始化：coupon / user_coupon（含你项目增量字段/索引）
USE `hm-promotion`;

-- 基础表（直接内联，避免依赖 mysql 客户端 SOURCE）
CREATE TABLE IF NOT EXISTS `coupon` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
    `name`           VARCHAR(64)  NOT NULL COMMENT '优惠券名称',
    `type`           TINYINT      NOT NULL DEFAULT 1 COMMENT '优惠类型 1=满减 2=折扣',
    `discount_value` INT          NOT NULL COMMENT '优惠值（满减金额分/折扣百分比如85）',
    `threshold`      INT          NOT NULL DEFAULT 0 COMMENT '使用门槛（分），0=无门槛',
    `scope_type`     TINYINT      NOT NULL DEFAULT 1 COMMENT '适用范围 1=全场 2=品牌 3=类目 4=商家',
    `publish_count`  INT          NOT NULL COMMENT '发行总量',
    `stock`          INT          NOT NULL COMMENT '剩余库存',
    `begin_time`     DATETIME     NOT NULL COMMENT '活动开始时间',
    `end_time`       DATETIME     NOT NULL COMMENT '活动结束时间',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1=草稿 2=进行中 3=已结束 4=暂停',
    `creator_id`     BIGINT       NULL COMMENT '创建者用户ID（管理员），由网关 user-info 传入',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`      BIGINT   NOT NULL COMMENT '领券用户ID',
    `coupon_id`    BIGINT   NOT NULL COMMENT '关联优惠券ID',
    `status`       TINYINT  NOT NULL DEFAULT 1 COMMENT '状态 1=未使用 2=已使用 3=已过期',
    `receive_time` DATETIME NOT NULL COMMENT '领取时间',
    `expired_at`   DATETIME NOT NULL COMMENT '过期时间',
    `order_id`     BIGINT   NULL COMMENT '订单ID',
    `use_time`     DATETIME NULL COMMENT '使用时间',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_coupon` (`user_id`, `coupon_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户领券记录表（由MQ消费者异步写入）';

-- 增量：以下脚本可重复执行（注意：MySQL SOURCE 只支持在客户端；在 entrypoint 中我们直接内联必要 DDL）

-- scope 关联表
CREATE TABLE IF NOT EXISTS `coupon_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `coupon_id` BIGINT NOT NULL,
  `category_name` VARCHAR(64) NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `coupon_brand` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `coupon_id` BIGINT NOT NULL,
  `brand_name` VARCHAR(64) NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `coupon_shop` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `coupon_id` BIGINT NOT NULL,
  `shop_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


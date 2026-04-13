-- 交易库业务表（与 trade-service 实体一致；表名 order 为 MySQL 保留字，须反引号）

USE `hm-trade`;

CREATE TABLE IF NOT EXISTS `order` (
  `id` BIGINT NOT NULL COMMENT '订单id',
  `total_fee` INT NOT NULL COMMENT '总金额，单位为分',
  `coupon_id` BIGINT NULL DEFAULT NULL COMMENT '优惠券ID',
  `discount_fee` INT NOT NULL DEFAULT 0 COMMENT '优惠金额（分）',
  `pay_fee` INT NOT NULL DEFAULT 0 COMMENT '应付金额（分）',
  `payment_type` INT NULL COMMENT '支付类型，1支付宝 2微信 3扣减余额',
  `user_id` BIGINT NULL COMMENT '用户id',
  `status` INT NULL COMMENT '订单状态',
  `create_time` DATETIME NULL DEFAULT NULL,
  `pay_time` DATETIME NULL DEFAULT NULL,
  `consign_time` DATETIME NULL DEFAULT NULL,
  `end_time` DATETIME NULL DEFAULT NULL,
  `close_time` DATETIME NULL DEFAULT NULL,
  `comment_time` DATETIME NULL DEFAULT NULL,
  `update_time` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status_create_time` (`status`, `create_time`),
  KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `order_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单详情id',
  `order_id` BIGINT NOT NULL COMMENT '订单id',
  `item_id` BIGINT NOT NULL COMMENT 'sku商品id',
  `num` INT NOT NULL COMMENT '购买数量',
  `name` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '商品标题',
  `spec` VARCHAR(1024) NULL DEFAULT NULL COMMENT '商品动态属性键值集',
  `price` INT NOT NULL COMMENT '价格,单位：分',
  `image` VARCHAR(512) NULL DEFAULT NULL COMMENT '商品图片',
  `create_time` DATETIME NULL DEFAULT NULL,
  `update_time` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单详情表';

CREATE TABLE IF NOT EXISTS `order_logistics` (
  `order_id` BIGINT NOT NULL COMMENT '订单id',
  `logistics_number` VARCHAR(64) NULL DEFAULT NULL COMMENT '物流单号',
  `logistics_company` VARCHAR(128) NULL DEFAULT NULL COMMENT '物流公司名称',
  `contact` VARCHAR(64) NULL DEFAULT NULL COMMENT '收件人',
  `mobile` VARCHAR(32) NULL DEFAULT NULL COMMENT '收件人手机号码',
  `province` VARCHAR(64) NULL DEFAULT NULL,
  `city` VARCHAR(64) NULL DEFAULT NULL,
  `town` VARCHAR(64) NULL DEFAULT NULL,
  `street` VARCHAR(256) NULL DEFAULT NULL,
  `create_time` DATETIME NULL DEFAULT NULL,
  `update_time` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单物流表';

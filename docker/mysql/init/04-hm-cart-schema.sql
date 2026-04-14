-- 购物车库：与 cart-service 实体一致

USE `hm-cart`;

CREATE TABLE IF NOT EXISTS `cart` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '购物车条目id',
  `user_id`     BIGINT       NOT NULL COMMENT '用户id',
  `item_id`     BIGINT       NOT NULL COMMENT 'sku 商品id',
  `num`         INT          NOT NULL DEFAULT 1 COMMENT '数量',
  `name`        VARCHAR(512) NOT NULL DEFAULT '' COMMENT '商品标题',
  `spec`        VARCHAR(1024) NULL DEFAULT NULL COMMENT '规格',
  `price`       INT          NOT NULL COMMENT '单价（分）',
  `image`       VARCHAR(512) NULL DEFAULT NULL COMMENT '图片',
  `create_time` DATETIME     NULL DEFAULT NULL,
  `update_time` DATETIME     NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车表';

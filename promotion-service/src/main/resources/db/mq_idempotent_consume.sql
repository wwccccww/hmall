-- 已有 hm-promotion 库增量：MQ 消费幂等表
CREATE TABLE IF NOT EXISTS `mq_idempotent_consume` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型，如 coupon_receive',
  `message_id` varchar(64) NOT NULL COMMENT '消息幂等键（UUID）',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_message` (`biz_type`, `message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MQ 消费幂等';

-- 支付库：与 pay-service PayOrder 实体一致（id 由应用侧 ASSIGN_ID 写入，无 AUTO_INCREMENT）

USE `hm-pay`;

CREATE TABLE IF NOT EXISTS `pay_order` (
  `id`               BIGINT       NOT NULL COMMENT '支付单主键（雪花等）',
  `biz_order_no`     BIGINT       NULL DEFAULT NULL COMMENT '业务订单号',
  `pay_order_no`     BIGINT       NULL DEFAULT NULL COMMENT '支付单号',
  `biz_user_id`      BIGINT       NULL DEFAULT NULL COMMENT '支付用户id',
  `pay_channel_code` VARCHAR(64)  NULL DEFAULT NULL COMMENT '支付渠道编码',
  `amount`           INT          NULL DEFAULT NULL COMMENT '金额（分）',
  `pay_type`         INT          NULL DEFAULT NULL COMMENT '支付类型 1~5',
  `status`           INT          NULL DEFAULT NULL COMMENT '0待提交 1待支付 2超时/取消 3成功',
  `expand_json`      VARCHAR(2048) NULL DEFAULT NULL COMMENT '拓展 JSON',
  `result_code`      VARCHAR(128) NULL DEFAULT NULL,
  `result_msg`       VARCHAR(512) NULL DEFAULT NULL,
  `pay_success_time` DATETIME     NULL DEFAULT NULL,
  `pay_over_time`    DATETIME     NULL DEFAULT NULL,
  `qr_code_url`      VARCHAR(1024) NULL DEFAULT NULL,
  `create_time`      DATETIME     NULL DEFAULT NULL,
  `update_time`      DATETIME     NULL DEFAULT NULL,
  `creater`          BIGINT       NULL DEFAULT NULL,
  `updater`          BIGINT       NULL DEFAULT NULL,
  `is_delete`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_biz_order_no` (`biz_order_no`),
  KEY `idx_biz_user_id` (`biz_user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付订单表';

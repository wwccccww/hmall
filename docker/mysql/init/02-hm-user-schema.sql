-- 用户库：与 user-service 实体一致（`user` 为常见保留语义，表名已加反引号）

USE `hm-user`;

CREATE TABLE IF NOT EXISTS `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
  `password`    VARCHAR(255) NOT NULL COMMENT '密码（BCrypt）',
  `phone`       VARCHAR(32)  NULL DEFAULT NULL COMMENT '手机号',
  `create_time` DATETIME     NULL DEFAULT NULL,
  `update_time` DATETIME     NULL DEFAULT NULL,
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '0=冻结 1=正常（与 UserStatus 枚举一致）',
  `balance`     INT          NOT NULL DEFAULT 0 COMMENT '余额（分）',
  `role`        INT          NOT NULL DEFAULT 0 COMMENT '0=普通用户 1=管理员',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `address` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
  `province`   VARCHAR(64)  NULL DEFAULT NULL,
  `city`       VARCHAR(64)  NULL DEFAULT NULL,
  `town`       VARCHAR(64)  NULL DEFAULT NULL,
  `mobile`     VARCHAR(32)  NULL DEFAULT NULL,
  `street`     VARCHAR(256) NULL DEFAULT NULL,
  `contact`    VARCHAR(64)  NULL DEFAULT NULL,
  `is_default` TINYINT      NOT NULL DEFAULT 0 COMMENT '1默认 0否',
  `notes`      VARCHAR(256) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户地址表';

-- 演示账号，密码均为 123456（BCrypt，与 Spring Security 兼容）；余额单位为「分」
INSERT INTO `user` (`id`, `username`, `password`, `phone`, `create_time`, `update_time`, `status`, `balance`, `role`)
VALUES
  (1, 'jack', '$2b$10$m6sR0DgYx3zBW7QVsj5TKOBr1nKsNB9hnUj6oDvpJ7IECtp.n.Wn2', '13688990011', NOW(), NOW(), 1, 1600000, 0),
  (2, 'admin', '$2b$10$m6sR0DgYx3zBW7QVsj5TKOBr1nKsNB9hnUj6oDvpJ7IECtp.n.Wn2', '13800138000', NOW(), NOW(), 1, 99999900, 1);

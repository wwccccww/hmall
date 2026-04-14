-- 商品库：与 item-service 实体一致（列 isAD 对应 Java 字段 isAD）

USE `hm-item`;

CREATE TABLE IF NOT EXISTS `item` (
  `id`            BIGINT       NOT NULL COMMENT '商品SKU id（可用雪花等，演示数据为固定 id）',
  `name`          VARCHAR(512) NOT NULL DEFAULT '' COMMENT 'SKU 名称',
  `price`         INT          NOT NULL COMMENT '价格（分）',
  `stock`         INT          NOT NULL DEFAULT 0 COMMENT '库存',
  `image`         VARCHAR(512) NULL DEFAULT NULL COMMENT '主图',
  `category`      VARCHAR(128) NULL DEFAULT NULL COMMENT '类目名称',
  `brand`         VARCHAR(128) NULL DEFAULT NULL COMMENT '品牌名称',
  `spec`          VARCHAR(1024) NULL DEFAULT NULL COMMENT '规格 JSON 或文案',
  `sold`          INT          NOT NULL DEFAULT 0 COMMENT '销量',
  `comment_count` INT          NOT NULL DEFAULT 0 COMMENT '评论数',
  `isAD`          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否推广',
  `status`        INT          NOT NULL DEFAULT 1 COMMENT '1正常 2下架 3删除',
  `create_time`   DATETIME     NULL DEFAULT NULL,
  `update_time`   DATETIME     NULL DEFAULT NULL,
  `creater`       BIGINT       NULL DEFAULT NULL,
  `updater`       BIGINT       NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_brand` (`brand`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品表';

-- 演示商品（可按需增删；库存仅供本地联调）
INSERT INTO `item` (`id`, `name`, `price`, `stock`, `image`, `category`, `brand`, `spec`, `sold`, `comment_count`, `isAD`, `status`, `create_time`, `update_time`, `creater`, `updater`)
VALUES
  (10001, '智能手机 Pro 256G', 399900, 500, 'https://picsum.photos/seed/hmall10001/400/400', '手机', '智品', '{"内存":"256G","颜色":"深空灰"}', 120, 36, 0, 1, NOW(), NOW(), 2, 2),
  (10002, '无线蓝牙耳机', 19900, 800, 'https://picsum.photos/seed/hmall10002/400/400', '数码配件', '声悦', '{"续航":"24h"}', 890, 210, 1, 1, NOW(), NOW(), 2, 2),
  (10003, '轻薄笔记本电脑', 529900, 120, 'https://picsum.photos/seed/hmall10003/400/400', '电脑', '极本', '{"屏幕":"14英寸","内存":"16G"}', 45, 12, 0, 1, NOW(), NOW(), 2, 2),
  (10004, '运动跑鞋 42码', 29900, 2000, 'https://picsum.photos/seed/hmall10004/400/400', '运动鞋', '疾风', '{"尺码":"42"}', 560, 88, 0, 1, NOW(), NOW(), 2, 2),
  (10005, '有机燕麦片 1kg', 4900, 3000, 'https://picsum.photos/seed/hmall10005/400/400', '食品', '谷语', NULL, 2300, 420, 0, 1, NOW(), NOW(), 2, 2)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

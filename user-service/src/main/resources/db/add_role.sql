-- 为 user 表增加 role 字段
-- 0 = 普通用户（默认）  1 = 管理员
ALTER TABLE `user`
    ADD COLUMN `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0=普通用户 1=管理员' AFTER `balance`;

-- 设置管理员账号（按实际用户名或 id 修改）
-- UPDATE `user` SET role = 1 WHERE username = 'admin';

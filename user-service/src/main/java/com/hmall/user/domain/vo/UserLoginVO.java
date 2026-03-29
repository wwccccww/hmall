package com.hmall.user.domain.vo;

import lombok.Data;

@Data
public class UserLoginVO {
    private String token;
    private Long userId;
    private String username;
    private Integer balance;
    /** 角色：0=普通用户  1=管理员 */
    private Integer role;
}

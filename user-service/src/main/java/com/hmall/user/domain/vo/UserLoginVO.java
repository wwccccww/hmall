package com.hmall.user.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录结果：token 供网关鉴权；userId 用于直连调试时填 user-info 请求头")
public class UserLoginVO {
    @Schema(description = "JWT，经网关时在 Authorization: Bearer 中携带")
    private String token;
    @Schema(description = "用户 ID，直连 user-service 调试其它接口时填入 user-info")
    private Long userId;
    private String username;
    private Integer balance;
    /** 角色：0=普通用户  1=管理员 */
    @Schema(description = "0=普通用户，1=管理员")
    private Integer role;
}

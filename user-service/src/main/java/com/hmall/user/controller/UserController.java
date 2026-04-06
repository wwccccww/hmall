package com.hmall.user.controller;


import com.hmall.user.domain.dto.LoginFormDTO;
import com.hmall.user.domain.vo.UserLoginVO;
import com.hmall.user.service.IUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.PageQuery;
import com.hmall.user.domain.po.User;
import com.hmall.user.config.UserOpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@Api(tags = "用户相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final IUserService userService;

    @Operation(summary = "用户登录", description = "无需 user-info。成功后可从响应 userId 填入 Authorize 的 user-info 调试其它接口。")
    @ApiOperation("用户登录接口")
    @SecurityRequirements
    @PostMapping("login")
    public UserLoginVO login(@RequestBody @Validated LoginFormDTO loginFormDTO){
        log.info("用户正在登录  {}", loginFormDTO.toString());
        return userService.login(loginFormDTO);
    }

    @Operation(summary = "扣减余额", description = "需请求头 user-info 为当前用户 ID")
    @ApiOperation("扣减余额")
    @SecurityRequirement(name = UserOpenApiConfiguration.USER_INFO_SCHEME)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pw", value = "支付密码"),
            @ApiImplicitParam(name = "amount", value = "支付金额")
    })
    @PutMapping("/money/deduct")
    public void deductMoney(@RequestParam("pw") String pw,@RequestParam("amount") Integer amount){
        userService.deductMoney(pw, amount);
    }
    @Operation(summary = "分页查询用户", description = "当前实现未按 user-info 过滤，分页参数见 PageQuery")
    @ApiOperation("分页查询用户")
    @GetMapping("/page")
    public PageDTO<User> queryUserByPage(PageQuery query) {
        Page<User> result = userService.page(query.toMpPage("update_time", false));
        return PageDTO.of(result, User.class);
    }

    @Operation(summary = "获取当前登录用户详情", description = "需请求头 user-info 为当前用户 ID")
    @ApiOperation("获取当前登录用户详情")
    @SecurityRequirement(name = UserOpenApiConfiguration.USER_INFO_SCHEME)
    @GetMapping("/me")
    public UserLoginVO queryMe() {
        User user = userService.getById(com.hmall.common.utils.UserContext.getUser());
        if (user == null) {
            return null;
        }
        UserLoginVO vo = com.hmall.common.utils.BeanUtils.copyBean(user, UserLoginVO.class);
        vo.setUserId(user.getId());
        return vo;
    }
}


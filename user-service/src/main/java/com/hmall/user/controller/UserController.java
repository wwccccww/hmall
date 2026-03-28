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

@Api(tags = "用户相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final IUserService userService;

    @ApiOperation("用户登录接口")
    @PostMapping("login")
    public UserLoginVO login(@RequestBody @Validated LoginFormDTO loginFormDTO){
        log.info("用户正在登录  {}", loginFormDTO.toString());
        return userService.login(loginFormDTO);
    }

    @ApiOperation("扣减余额")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pw", value = "支付密码"),
            @ApiImplicitParam(name = "amount", value = "支付金额")
    })
    @PutMapping("/money/deduct")
    public void deductMoney(@RequestParam("pw") String pw,@RequestParam("amount") Integer amount){
        userService.deductMoney(pw, amount);
    }
    @ApiOperation("分页查询用户")
    @GetMapping("/page")
    public PageDTO<User> queryUserByPage(PageQuery query) {
        Page<User> result = userService.page(query.toMpPage("update_time", false));
        return PageDTO.of(result, User.class);
    }

    @ApiOperation("获取当前登录用户详情")
    @GetMapping("/me")
    public UserLoginVO queryMe(){
        return com.hmall.common.utils.BeanUtils.copyBean(userService.getById(com.hmall.common.utils.UserContext.getUser()), UserLoginVO.class);
    }
}


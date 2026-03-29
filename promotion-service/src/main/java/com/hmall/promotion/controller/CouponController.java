package com.hmall.promotion.controller;

import com.hmall.promotion.domain.dto.CouponFormDTO;
import com.hmall.promotion.domain.vo.CouponVO;
import com.hmall.promotion.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "优惠券相关接口")
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final ICouponService couponService;

    @ApiOperation("创建优惠券（管理端）")
    @PostMapping
    public Long createCoupon(@Valid @RequestBody CouponFormDTO form) {
        return couponService.createCoupon(form);
    }

    @ApiOperation("发布优惠券（管理端）：将库存同步至 Redis，状态改为进行中")
    @PutMapping("/{id}/publish")
    public void publishCoupon(@PathVariable("id") Long id) {
        couponService.publishCoupon(id);
    }

    @ApiOperation("查询所有进行中的优惠券列表")
    @GetMapping
    public List<CouponVO> queryAvailableCoupons() {
        return couponService.queryAvailableCoupons();
    }

    @ApiOperation("用户抢券（秒杀）：Lua 原子扣减 Redis 库存，成功后异步落库")
    @PostMapping("/{id}/receive")
    public void receiveCoupon(@PathVariable("id") Long id) {
        couponService.receiveCoupon(id);
    }

    @ApiOperation("查询我的优惠券列表")
    @GetMapping("/my")
    public List<CouponVO> queryMyCoupons() {
        return couponService.queryMyCoupons();
    }
}

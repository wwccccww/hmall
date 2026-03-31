package com.hmall.promotion.controller;

import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.PageQuery;
import com.hmall.promotion.domain.dto.CouponFormDTO;
import com.hmall.promotion.domain.dto.CouponAvailableRequest;
import com.hmall.promotion.domain.dto.CouponPreviewRequest;
import com.hmall.promotion.domain.dto.CouponRedeemRequest;
import com.hmall.promotion.domain.vo.AvailableCouponVO;
import com.hmall.promotion.domain.vo.CouponPreviewVO;
import com.hmall.promotion.domain.vo.CouponReceiveRecordVO;
import com.hmall.promotion.domain.vo.CouponVO;
import com.hmall.promotion.domain.vo.MyCouponVO;
import com.hmall.promotion.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

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

    @ApiOperation("查询所有进行中的优惠券列表（C 端公开）")
    @GetMapping
    public List<CouponVO> queryAvailableCoupons() {
        return couponService.queryAvailableCoupons();
    }

    @ApiOperation("管理端：查询当前管理员创建的优惠券（全部状态）")
    @GetMapping("/manage")
    public List<CouponVO> queryManageCoupons() {
        return couponService.queryManageCoupons();
    }

    @ApiOperation("管理端：分页查询某张券的领取记录（user_coupon，仅创建者可查）")
    @GetMapping("/{id}/records")
    public PageDTO<CouponReceiveRecordVO> queryCouponReceiveRecords(
            @PathVariable("id") Long id,
            PageQuery pageQuery) {
        return couponService.queryCouponReceiveRecords(id, pageQuery);
    }

    @ApiOperation("用户抢券（秒杀）：Lua 原子扣减 Redis 库存，成功后异步落库")
    @PostMapping("/{id}/receive")
    public void receiveCoupon(@PathVariable("id") Long id) {
        couponService.receiveCoupon(id);
    }

    @ApiOperation("查询我的优惠券列表")
    @GetMapping("/my")
    public List<MyCouponVO> queryMyCoupons() {
        return couponService.queryMyCoupons();
    }

    @ApiOperation("购物车可用券：根据商品明细返回可用券列表（含立减金额）")
    @PostMapping("/available")
    public List<AvailableCouponVO> queryAvailableForCart(@Valid @RequestBody CouponAvailableRequest request) {
        return couponService.queryAvailableForCart(request);
    }

    @ApiOperation("试算优惠券：根据商品明细计算优惠金额与应付金额（需登录且已领券）")
    @PostMapping("/preview")
    public CouponPreviewVO previewCoupon(@Valid @RequestBody CouponPreviewRequest request) {
        return couponService.previewCoupon(request);
    }

    @ApiOperation("核销用券：支付成功后调用，将用户券置为已使用并记录订单号")
    @PostMapping("/redeem")
    public void redeemCoupon(@Valid @RequestBody CouponRedeemRequest request) {
        couponService.redeemCoupon(request);
    }

    @ApiOperation("批量获取券的 Redis 实时剩余库存（前端实时轮询）")
    @GetMapping("/stock")
    public Map<Long, Integer> getRealtimeStock(@RequestParam("ids") List<Long> ids) {
        return couponService.getRealtimeStock(ids);
    }
}

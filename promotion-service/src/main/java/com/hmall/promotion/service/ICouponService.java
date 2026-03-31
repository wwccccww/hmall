package com.hmall.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.PageQuery;
import com.hmall.promotion.domain.dto.CouponFormDTO;
import com.hmall.promotion.domain.dto.CouponPreviewRequest;
import com.hmall.promotion.domain.dto.CouponRedeemRequest;
import com.hmall.promotion.domain.dto.CouponAvailableRequest;
import com.hmall.promotion.domain.po.Coupon;
import com.hmall.promotion.domain.vo.CouponVO;
import com.hmall.promotion.domain.vo.CouponReceiveRecordVO;
import com.hmall.promotion.domain.vo.CouponPreviewVO;
import com.hmall.promotion.domain.vo.MyCouponVO;
import com.hmall.promotion.domain.vo.AvailableCouponVO;

import java.util.List;
import java.util.Map;

public interface ICouponService extends IService<Coupon> {

    /** 创建优惠券（状态=草稿），返回新 ID */
    Long createCoupon(CouponFormDTO form);

    /**
     * 发布优惠券：状态 1→2，并将库存同步到 Redis。
     * 同步后 Redis 中维护两个 key：
     *   promotion:coupon:stock:{id}   → 剩余库存（String/Integer）
     *   promotion:coupon:user:{id}    → 已领用户 Set（Set<String>）
     */
    void publishCoupon(Long id);

    /**
     * 用户抢券（秒杀主流程）：
     * 1. Lua 脚本原子判断库存 + 用户去重
     * 2. 校验通过后发 MQ 消息，由消费者异步落库
     *
     * <p>三重判断：
     * <ul>
     *   <li>库存不足 → 抛出业务异常</li>
     *   <li>用户已领  → 抛出业务异常</li>
     *   <li>成功       → 发 MQ，主线程立即返回</li>
     * </ul>
     */
    void receiveCoupon(Long couponId);

    /** 查询当前用户的全部领券记录 */
    List<MyCouponVO> queryMyCoupons();

    /** 购物车可用券：输入商品明细，返回可用券列表（含立减金额试算） */
    List<AvailableCouponVO> queryAvailableForCart(CouponAvailableRequest request);

    /** 管理端：分页查询某张券的领取记录（user_coupon） */
    PageDTO<CouponReceiveRecordVO> queryCouponReceiveRecords(Long couponId, PageQuery pageQuery);

    /** C端：试算指定优惠券在当前商品列表下的优惠金额 */
    CouponPreviewVO previewCoupon(CouponPreviewRequest request);

    /** C端：核销用券（支付成功后调用） */
    void redeemCoupon(CouponRedeemRequest request);

    /** 查询所有进行中的优惠券列表（C 端领券中心，展示全平台已发布券） */
    List<CouponVO> queryAvailableCoupons();

    /** 管理端：当前登录管理员创建的优惠券（含草稿/进行中/结束等全部状态） */
    List<CouponVO> queryManageCoupons();

    /**
     * 从 Redis 获取多张券的实时剩余库存。
     * 若 Redis key 不存在（券未发布或已清除），返回值为 0。
     */
    Map<Long, Integer> getRealtimeStock(List<Long> ids);

    /** 定时任务：把活动已结束但状态仍为进行中(2)的券自动置为已结束(3) */
    void autoExpireCoupons();
}

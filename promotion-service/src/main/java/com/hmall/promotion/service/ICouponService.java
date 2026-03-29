package com.hmall.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.promotion.domain.dto.CouponFormDTO;
import com.hmall.promotion.domain.po.Coupon;
import com.hmall.promotion.domain.vo.CouponVO;

import java.util.List;

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
    List<CouponVO> queryMyCoupons();

    /** 查询所有进行中的优惠券列表 */
    List<CouponVO> queryAvailableCoupons();
}

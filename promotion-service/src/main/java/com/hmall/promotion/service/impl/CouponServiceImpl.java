package com.hmall.promotion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.UserContext;
import com.hmall.promotion.domain.dto.CouponFormDTO;
import com.hmall.promotion.domain.po.Coupon;
import com.hmall.promotion.domain.po.UserCoupon;
import com.hmall.promotion.domain.vo.CouponVO;
import com.hmall.promotion.mapper.CouponMapper;
import com.hmall.promotion.mq.CouponReceiveMessage;
import com.hmall.promotion.service.ICouponService;
import com.hmall.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 优惠券服务实现类。
 *
 * <p><b>秒杀核心流程（receiveCoupon）</b>：
 * <pre>
 *  用户请求 → Lua 脚本（Redis 原子操作）→ 判断结果
 *      ├─ 库存不足  → 抛异常（用户收到提示）
 *      ├─ 重复领取  → 抛异常（用户收到提示）
 *      └─ 成功      → 发 RabbitMQ 消息 → 消费者异步写 user_coupon 表
 * </pre>
 *
 * <p>Lua 脚本保证"判断库存 + 扣减库存 + 记录用户"三步原子执行，彻底消除并发超卖问题。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

    /** 库存 Redis key 前缀 */
    private static final String COUPON_STOCK_KEY = "promotion:coupon:stock:";
    /** 已领用户 Set Redis key 前缀 */
    private static final String COUPON_USER_KEY  = "promotion:coupon:user:";

    /** MQ Exchange（Topic 类型） */
    public static final String EXCHANGE    = "promotion.topic";
    /** 抢券成功路由键 */
    public static final String ROUTING_KEY = "coupon.receive";

    /** 预加载 Lua 脚本（只加载一次，带 SHA 缓存） */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("scripts/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate      rabbitTemplate;
    private final IUserCouponService  userCouponService;

    // ======================== 管理端操作 ========================

    @Override
    public Long createCoupon(CouponFormDTO form) {
        Coupon coupon = BeanUtils.copyBean(form, Coupon.class);
        coupon.setStock(form.getPublishCount()); // 初始库存 = 发行总量
        coupon.setStatus(1);                     // 草稿状态
        save(coupon);
        return coupon.getId();
    }

    @Override
    public void publishCoupon(Long id) {
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new BizIllegalException("优惠券不存在");
        }
        if (coupon.getStatus() != 1) {
            throw new BizIllegalException("只有草稿状态的优惠券可以发布");
        }
        // 1. 更新 DB 状态
        Coupon update = new Coupon();
        update.setId(id);
        update.setStatus(2);
        updateById(update);

        // 2. 同步库存到 Redis（后续 receiveCoupon 直接操作 Redis，不访问 DB）
        String stockKey = COUPON_STOCK_KEY + id;
        String userKey  = COUPON_USER_KEY  + id;
        redisTemplate.opsForValue().set(stockKey, coupon.getStock().toString());
        // 清理上次可能残留的用户 Set（幂等发布）
        redisTemplate.delete(userKey);
        log.info("优惠券已发布，id={}, 库存={}", id, coupon.getStock());
    }

    // ======================== 用户抢券（核心秒杀逻辑） ========================

    @Override
    public void receiveCoupon(Long couponId) {
        Long userId = UserContext.getUser();

        // 1. 查询优惠券基本信息（需要 expiredAt 写入 MQ 消息）
        Coupon coupon = getById(couponId);
        if (coupon == null || coupon.getStatus() != 2) {
            throw new BizIllegalException("优惠券不存在或活动未开始");
        }

        // 2. 执行 Lua 脚本：原子判断库存 + 用户去重 + 扣减
        String stockKey = COUPON_STOCK_KEY + couponId;
        String userKey  = COUPON_USER_KEY  + couponId;
        Long result = redisTemplate.execute(
                SECKILL_SCRIPT,
                List.of(stockKey, userKey),
                userId.toString()
        );

        // 3. 根据脚本返回值判断结果
        if (result == null || result == 1L) {
            throw new BizIllegalException("优惠券已抢完，下次早点来！");
        }
        if (result == 2L) {
            throw new BizIllegalException("每人限领一张，您已领取过该优惠券");
        }

        // 4. 抢券成功：发送 MQ 消息，由消费者异步写入 user_coupon 表
        CouponReceiveMessage message = new CouponReceiveMessage(
                userId, couponId, coupon.getEndTime());
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, message);
        log.info("用户 {} 抢券成功，couponId={}，已发送 MQ 消息", userId, couponId);
    }

    // ======================== 查询 ========================

    @Override
    public List<CouponVO> queryMyCoupons() {
        Long userId = UserContext.getUser();
        List<UserCoupon> records = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .list();
        List<Long> couponIds = records.stream()
                .map(UserCoupon::getCouponId)
                .collect(Collectors.toList());
        if (couponIds.isEmpty()) {
            return List.of();
        }
        return BeanUtils.copyList(listByIds(couponIds), CouponVO.class);
    }

    @Override
    public List<CouponVO> queryAvailableCoupons() {
        List<Coupon> coupons = lambdaQuery()
                .eq(Coupon::getStatus, 2)
                .list();
        return BeanUtils.copyList(coupons, CouponVO.class);
    }
}

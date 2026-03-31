package com.hmall.promotion.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.PageQuery;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.exception.UnauthorizedException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.UserContext;
import com.hmall.promotion.domain.dto.CouponAvailableRequest;
import com.hmall.promotion.domain.dto.CouponFormDTO;
import com.hmall.promotion.domain.dto.CouponItemDTO;
import com.hmall.promotion.domain.dto.CouponPreviewRequest;
import com.hmall.promotion.domain.dto.CouponRedeemRequest;
import com.hmall.promotion.domain.po.CouponBrand;
import com.hmall.promotion.domain.po.CouponCategory;
import com.hmall.promotion.domain.po.CouponShop;
import com.hmall.promotion.domain.po.Coupon;
import com.hmall.promotion.domain.po.UserCoupon;
import com.hmall.promotion.domain.vo.AvailableCouponVO;
import com.hmall.promotion.domain.vo.CouponReceiveRecordVO;
import com.hmall.promotion.domain.vo.CouponPreviewVO;
import com.hmall.promotion.domain.vo.CouponVO;
import com.hmall.promotion.domain.vo.MyCouponVO;
import com.hmall.promotion.mapper.CouponBrandMapper;
import com.hmall.promotion.mapper.CouponCategoryMapper;
import com.hmall.promotion.mapper.CouponMapper;
import com.hmall.promotion.mapper.CouponShopMapper;
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

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final CouponCategoryMapper couponCategoryMapper;
    private final CouponBrandMapper couponBrandMapper;
    private final CouponShopMapper couponShopMapper;

    // ======================== 管理端操作 ========================

    @Override
    public Long createCoupon(CouponFormDTO form) {
        Long creatorId = UserContext.getUser();
        if (creatorId == null) {
            throw new UnauthorizedException("请先登录后再创建优惠券");
        }
        Integer scopeType = form.getScopeType() == null ? 1 : form.getScopeType();
        if (!Objects.equals(scopeType, 1) && !Objects.equals(scopeType, 2) && !Objects.equals(scopeType, 3) && !Objects.equals(scopeType, 4)) {
            throw new BizIllegalException("无效的适用范围 scopeType");
        }
        if (Objects.equals(scopeType, 3)) {
            List<String> categoryNames = form.getCategoryNames();
            boolean empty = categoryNames == null || categoryNames.stream().noneMatch(s -> s != null && !s.trim().isEmpty());
            if (empty) {
                throw new BizIllegalException("指定类目券必须配置 categoryNames");
            }
        }
        if (Objects.equals(scopeType, 2)) {
            List<String> brandNames = form.getBrandNames();
            boolean empty = brandNames == null || brandNames.stream().noneMatch(s -> s != null && !s.trim().isEmpty());
            if (empty) {
                throw new BizIllegalException("指定品牌券必须配置 brandNames");
            }
        }
        if (Objects.equals(scopeType, 4)) {
            List<Long> shopIds = form.getShopIds();
            if (shopIds == null || shopIds.isEmpty()) {
                throw new BizIllegalException("指定商家券必须配置 shopIds");
            }
        }
        Coupon coupon = BeanUtils.copyBean(form, Coupon.class);
        coupon.setStock(form.getPublishCount()); // 初始库存 = 发行总量
        coupon.setStatus(1);                     // 草稿状态
        coupon.setCreatorId(creatorId);
        coupon.setScopeType(scopeType);
        save(coupon);
        if (Objects.equals(scopeType, 3)) {
            List<String> names = form.getCategoryNames().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            for (String name : names) {
                CouponCategory cc = new CouponCategory();
                cc.setCouponId(coupon.getId());
                cc.setCategoryName(name);
                couponCategoryMapper.insert(cc);
            }
        }
        if (Objects.equals(scopeType, 2)) {
            List<String> names = form.getBrandNames().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            for (String name : names) {
                CouponBrand cb = new CouponBrand();
                cb.setCouponId(coupon.getId());
                cb.setBrandName(name);
                couponBrandMapper.insert(cb);
            }
        }
        if (Objects.equals(scopeType, 4)) {
            List<Long> ids = form.getShopIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            for (Long sid : ids) {
                CouponShop cs = new CouponShop();
                cs.setCouponId(coupon.getId());
                cs.setShopId(sid);
                couponShopMapper.insert(cs);
            }
        }
        return coupon.getId();
    }

    @Override
    public void publishCoupon(Long id) {
        Long uid = UserContext.getUser();
        if (uid == null) {
            throw new UnauthorizedException("请先登录");
        }
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new BizIllegalException("优惠券不存在");
        }
        if (coupon.getCreatorId() != null && !coupon.getCreatorId().equals(uid)) {
            throw new BizIllegalException("无权发布他人创建的优惠券");
        }
        if (coupon.getStatus() != 1) {
            throw new BizIllegalException("只有草稿状态的优惠券可以发布");
        }
        Integer scopeType = coupon.getScopeType() == null ? 1 : coupon.getScopeType();
        if (Objects.equals(scopeType, 3)) {
            Long cnt = couponCategoryMapper.selectCount(Wrappers.<CouponCategory>lambdaQuery()
                    .eq(CouponCategory::getCouponId, id));
            if (cnt == null || cnt <= 0) {
                throw new BizIllegalException("指定类目券未配置类目，无法发布");
            }
        }
        if (Objects.equals(scopeType, 2)) {
            Long cnt = couponBrandMapper.selectCount(Wrappers.<CouponBrand>lambdaQuery()
                    .eq(CouponBrand::getCouponId, id));
            if (cnt == null || cnt <= 0) {
                throw new BizIllegalException("指定品牌券未配置品牌，无法发布");
            }
        }
        if (Objects.equals(scopeType, 4)) {
            Long cnt = couponShopMapper.selectCount(Wrappers.<CouponShop>lambdaQuery()
                    .eq(CouponShop::getCouponId, id));
            if (cnt == null || cnt <= 0) {
                throw new BizIllegalException("指定商家券未配置商家，无法发布");
            }
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
    public List<MyCouponVO> queryMyCoupons() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new UnauthorizedException("请先登录");
        }
        LocalDateTime now = LocalDateTime.now();
        // 只返回：未使用(1) 且未过期
        List<UserCoupon> records = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, 1)
                .gt(UserCoupon::getExpiredAt, now)
                .list();
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        List<Long> couponIds = records.stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
        Map<Long, UserCoupon> recordMap = records.stream().collect(Collectors.toMap(UserCoupon::getCouponId, r -> r, (a, b) -> a));
        List<Coupon> coupons = listByIds(couponIds);
        List<MyCouponVO> out = BeanUtils.copyList(coupons, MyCouponVO.class);
        for (MyCouponVO vo : out) {
            UserCoupon r = recordMap.get(vo.getId());
            if (r != null) {
                vo.setUserCouponStatus(r.getStatus());
                vo.setReceiveTime(r.getReceiveTime());
                vo.setExpiredAt(r.getExpiredAt());
            }
            fillScopeDetail(vo);
        }
        return out;
    }

    @Override
    public List<AvailableCouponVO> queryAvailableForCart(CouponAvailableRequest request) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new UnauthorizedException("请先登录");
        }
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> records = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, 1)
                .gt(UserCoupon::getExpiredAt, now)
                .list();
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        List<Long> couponIds = records.stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
        Map<Long, UserCoupon> recordMap = records.stream().collect(Collectors.toMap(UserCoupon::getCouponId, r -> r, (a, b) -> a));
        List<Coupon> coupons = listByIds(couponIds).stream()
                .filter(c -> c != null && Objects.equals(c.getStatus(), 2))
                .filter(c -> c.getBeginTime() == null || !now.isBefore(c.getBeginTime()))
                .filter(c -> c.getEndTime() == null || now.isBefore(c.getEndTime()))
                .collect(Collectors.toList());

        List<AvailableCouponVO> out = BeanUtils.copyList(coupons, AvailableCouponVO.class);
        java.util.ArrayList<AvailableCouponVO> usable = new java.util.ArrayList<>();
        for (AvailableCouponVO vo : out) {
            UserCoupon r = recordMap.get(vo.getId());
            if (r != null) {
                vo.setReceiveTime(r.getReceiveTime());
                vo.setExpiredAt(r.getExpiredAt());
            }
            fillScopeDetail(vo);
            Coupon c = coupons.stream().filter(x -> Objects.equals(x.getId(), vo.getId())).findFirst().orElse(null);
            if (c == null) continue;
            AmountCalc calc = calcAmounts(c, request.getItems());
            if (calc.eligibleAmount < safeInt(c.getThreshold())) continue;
            int discount = calcDiscount(c, calc.eligibleAmount);
            if (discount <= 0) continue;
            vo.setEligibleAmount(calc.eligibleAmount);
            vo.setDiscountAmount(discount);
            vo.setPayAmount(Math.max(calc.totalAmount - discount, 0));
            usable.add(vo);
        }
        usable.sort((a, b) -> safeInt(b.getDiscountAmount()) - safeInt(a.getDiscountAmount()));
        return usable;
    }

    @Override
    public CouponPreviewVO previewCoupon(CouponPreviewRequest request) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new UnauthorizedException("请先登录");
        }
        Coupon coupon = getById(request.getCouponId());
        if (coupon == null) {
            throw new BizIllegalException("优惠券不存在");
        }
        if (coupon.getStatus() != 2) {
            throw new BizIllegalException("优惠券不可用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getBeginTime() != null && now.isBefore(coupon.getBeginTime())) {
            throw new BizIllegalException("活动未开始");
        }
        if (coupon.getEndTime() != null && !now.isBefore(coupon.getEndTime())) {
            throw new BizIllegalException("活动已结束");
        }
        // 必须已领且未使用
        UserCoupon uc = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, coupon.getId())
                .one();
        if (uc == null) {
            throw new BizIllegalException("请先领取该优惠券");
        }
        if (!Objects.equals(uc.getStatus(), 1)) {
            throw new BizIllegalException("该优惠券已使用或已失效");
        }

        AmountCalc calc = calcAmounts(coupon, request.getItems());
        if (calc.eligibleAmount < safeInt(coupon.getThreshold())) {
            throw new BizIllegalException("未满足使用门槛");
        }
        int discount = calcDiscount(coupon, calc.eligibleAmount);
        int pay = Math.max(calc.totalAmount - discount, 0);

        CouponPreviewVO vo = new CouponPreviewVO();
        vo.setCouponId(coupon.getId());
        vo.setTotalAmount(calc.totalAmount);
        vo.setEligibleAmount(calc.eligibleAmount);
        vo.setDiscountAmount(discount);
        vo.setPayAmount(pay);
        return vo;
    }

    @Override
    public void redeemCoupon(CouponRedeemRequest request) {
        Long userId = request.getUserId();
        if (userId == null) throw new UnauthorizedException("请先登录");
        Coupon coupon = getById(request.getCouponId());
        if (coupon == null) {
            throw new BizIllegalException("优惠券不存在");
        }
        AmountCalc calc = calcAmounts(coupon, request.getItems());
        if (calc.eligibleAmount < safeInt(coupon.getThreshold())) {
            throw new BizIllegalException("未满足使用门槛");
        }
        // 核销：仅允许从 未使用(1) -> 已使用(2)，并记录订单号与使用时间
        boolean ok = userCouponService.lambdaUpdate()
                .set(UserCoupon::getStatus, 2)
                .set(UserCoupon::getOrderId, request.getOrderId())
                .set(UserCoupon::getUseTime, LocalDateTime.now())
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, request.getCouponId())
                .eq(UserCoupon::getStatus, 1)
                .update();
        if (!ok) {
            throw new BizIllegalException("优惠券核销失败（可能已使用）");
        }
    }

    @Override
    public PageDTO<CouponReceiveRecordVO> queryCouponReceiveRecords(Long couponId, PageQuery pageQuery) {
        Long creatorId = UserContext.getUser();
        if (creatorId == null) {
            throw new UnauthorizedException("请先登录");
        }
        Coupon coupon = getById(couponId);
        if (coupon == null) {
            throw new BizIllegalException("优惠券不存在");
        }
        if (coupon.getCreatorId() != null && !coupon.getCreatorId().equals(creatorId)) {
            throw new BizIllegalException("无权查看他人创建的优惠券领取记录");
        }
        Page<UserCoupon> page = userCouponService.lambdaQuery()
                .eq(UserCoupon::getCouponId, couponId)
                .page(pageQuery.toMpPage("receive_time", false));
        return PageDTO.of(page, CouponReceiveRecordVO.class);
    }

    @Override
    public List<CouponVO> queryAvailableCoupons() {
        List<Coupon> coupons = lambdaQuery()
                .eq(Coupon::getStatus, 2)
                .list();
        List<CouponVO> vos = BeanUtils.copyList(coupons, CouponVO.class);
        enrichWithRedisStock(vos);
        return vos;
    }

    @Override
    public List<CouponVO> queryManageCoupons() {
        Long creatorId = UserContext.getUser();
        if (creatorId == null) {
            throw new UnauthorizedException("请先登录");
        }
        List<Coupon> coupons = list(
                Wrappers.<Coupon>lambdaQuery()
                        .eq(Coupon::getCreatorId, creatorId)
                        .orderByDesc(Coupon::getCreateTime));
        List<CouponVO> vos = BeanUtils.copyList(coupons, CouponVO.class);
        enrichWithRedisStock(vos);
        return vos;
    }

    @Override
    public Map<Long, Integer> getRealtimeStock(List<Long> ids) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Long id : ids) {
            String s = redisTemplate.opsForValue().get(COUPON_STOCK_KEY + id);
            result.put(id, s != null ? safeParseInt(s) : 0);
        }
        return result;
    }

    @Override
    public void autoExpireCoupons() {
        List<Coupon> expired = lambdaQuery()
                .eq(Coupon::getStatus, 2)
                .lt(Coupon::getEndTime, LocalDateTime.now())
                .list();
        if (expired.isEmpty()) return;
        List<Long> ids = expired.stream().map(Coupon::getId).collect(Collectors.toList());
        update(Wrappers.<Coupon>lambdaUpdate()
                .set(Coupon::getStatus, 3)
                .in(Coupon::getId, ids));
        log.info("自动将 {} 张优惠券置为已结束: {}", ids.size(), ids);
    }

    // ======================== 私有工具 ========================

    /** 从 Redis 读取实时库存并回写到 CouponVO 列表（已发布的券才有 Redis key） */
    private void enrichWithRedisStock(List<CouponVO> vos) {
        for (CouponVO vo : vos) {
            String s = redisTemplate.opsForValue().get(COUPON_STOCK_KEY + vo.getId());
            if (s != null) {
                vo.setStock(safeParseInt(s));
            }
        }
    }

    private int safeParseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private int safeInt(Integer v) {
        return v == null ? 0 : v;
    }

    private static class AmountCalc {
        private final int totalAmount;
        private final int eligibleAmount;

        private AmountCalc(int totalAmount, int eligibleAmount) {
            this.totalAmount = totalAmount;
            this.eligibleAmount = eligibleAmount;
        }
    }

    private AmountCalc calcAmounts(Coupon coupon, List<CouponItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new BizIllegalException("商品明细不能为空");
        }
        int total = 0;
        for (CouponItemDTO it : items) {
            int price = safeInt(it.getPrice());
            int num = safeInt(it.getNum());
            total += price * num;
        }
        int eligible;
        Integer scopeType = coupon.getScopeType() == null ? 1 : coupon.getScopeType();
        if (Objects.equals(scopeType, 1)) {
            eligible = total;
        } else if (Objects.equals(scopeType, 2)) {
            List<CouponBrand> rows = couponBrandMapper.selectList(
                    Wrappers.<CouponBrand>lambdaQuery().eq(CouponBrand::getCouponId, coupon.getId()));
            Set<String> allow = rows.stream()
                    .map(CouponBrand::getBrandName)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            eligible = 0;
            for (CouponItemDTO it : items) {
                String b = it.getBrand() == null ? "" : it.getBrand().trim();
                if (allow.contains(b)) {
                    eligible += safeInt(it.getPrice()) * safeInt(it.getNum());
                }
            }
        } else if (Objects.equals(scopeType, 3)) {
            List<CouponCategory> rows = couponCategoryMapper.selectList(
                    Wrappers.<CouponCategory>lambdaQuery().eq(CouponCategory::getCouponId, coupon.getId()));
            Set<String> allow = rows.stream()
                    .map(CouponCategory::getCategoryName)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            eligible = 0;
            for (CouponItemDTO it : items) {
                String c = it.getCategory() == null ? "" : it.getCategory().trim();
                if (allow.contains(c)) {
                    eligible += safeInt(it.getPrice()) * safeInt(it.getNum());
                }
            }
        } else if (Objects.equals(scopeType, 4)) {
            List<CouponShop> rows = couponShopMapper.selectList(
                    Wrappers.<CouponShop>lambdaQuery().eq(CouponShop::getCouponId, coupon.getId()));
            Set<Long> allow = rows.stream()
                    .map(CouponShop::getShopId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            eligible = 0;
            for (CouponItemDTO it : items) {
                if (it.getShopId() != null && allow.contains(it.getShopId())) {
                    eligible += safeInt(it.getPrice()) * safeInt(it.getNum());
                }
            }
        } else {
            throw new BizIllegalException("无效的适用范围");
        }
        return new AmountCalc(total, eligible);
    }

    private void fillScopeDetail(MyCouponVO vo) {
        Integer scopeType = vo.getScopeType() == null ? 1 : vo.getScopeType();
        if (Objects.equals(scopeType, 3)) {
            vo.setCategoryNames(couponCategoryMapper.selectList(Wrappers.<CouponCategory>lambdaQuery()
                            .eq(CouponCategory::getCouponId, vo.getId()))
                    .stream().map(CouponCategory::getCategoryName).collect(Collectors.toList()));
        } else if (Objects.equals(scopeType, 2)) {
            vo.setBrandNames(couponBrandMapper.selectList(Wrappers.<CouponBrand>lambdaQuery()
                            .eq(CouponBrand::getCouponId, vo.getId()))
                    .stream().map(CouponBrand::getBrandName).collect(Collectors.toList()));
        } else if (Objects.equals(scopeType, 4)) {
            vo.setShopIds(couponShopMapper.selectList(Wrappers.<CouponShop>lambdaQuery()
                            .eq(CouponShop::getCouponId, vo.getId()))
                    .stream().map(CouponShop::getShopId).collect(Collectors.toList()));
        }
    }

    private void fillScopeDetail(AvailableCouponVO vo) {
        Integer scopeType = vo.getScopeType() == null ? 1 : vo.getScopeType();
        if (Objects.equals(scopeType, 3)) {
            vo.setCategoryNames(couponCategoryMapper.selectList(Wrappers.<CouponCategory>lambdaQuery()
                            .eq(CouponCategory::getCouponId, vo.getId()))
                    .stream().map(CouponCategory::getCategoryName).collect(Collectors.toList()));
        } else if (Objects.equals(scopeType, 2)) {
            vo.setBrandNames(couponBrandMapper.selectList(Wrappers.<CouponBrand>lambdaQuery()
                            .eq(CouponBrand::getCouponId, vo.getId()))
                    .stream().map(CouponBrand::getBrandName).collect(Collectors.toList()));
        } else if (Objects.equals(scopeType, 4)) {
            vo.setShopIds(couponShopMapper.selectList(Wrappers.<CouponShop>lambdaQuery()
                            .eq(CouponShop::getCouponId, vo.getId()))
                    .stream().map(CouponShop::getShopId).collect(Collectors.toList()));
        }
    }

    private int calcDiscount(Coupon coupon, int eligibleAmount) {
        if (eligibleAmount <= 0) return 0;
        int type = safeInt(coupon.getType());
        int discountValue = safeInt(coupon.getDiscountValue());
        if (type == 1) { // 满减，discountValue 为分
            return Math.min(discountValue, eligibleAmount);
        }
        if (type == 2) { // 折扣，discountValue 为 85 表示 85 折
            int percent = Math.max(1, Math.min(discountValue, 99));
            return eligibleAmount - (eligibleAmount * percent / 100);
        }
        return 0;
    }
}

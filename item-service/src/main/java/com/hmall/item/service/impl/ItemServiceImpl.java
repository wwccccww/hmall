package com.hmall.item.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.item.domain.dto.ItemDTO;
import com.hmall.item.domain.dto.OrderDetailDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.mapper.ItemMapper;
import com.hmall.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现类。
 *
 * <p>queryItemById 引入了 Redis 缓存三件套：
 * <ul>
 *   <li><b>防穿透</b>：DB 查无数据时，向 Redis 写空值（空字符串），设置短 TTL，避免大量无效请求打到 DB。</li>
 *   <li><b>防击穿</b>：热点 key 过期时，通过 SETNX 互斥锁保证只有一个线程重建缓存，其他线程等待重试。</li>
 *   <li><b>防雪崩</b>：缓存 TTL = 基础时间 + 随机偏移量，使不同 key 的过期时间分散，降低同时失效的风险。</li>
 * </ul>
 *
 * <p>写操作（updateById / removeById / deductStock）采用"先写 DB，再删缓存"策略保证最终一致性。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    private static final String ITEM_CACHE_KEY      = "item:cache:";
    private static final String ITEM_LOCK_KEY       = "item:lock:";
    /** 正常缓存 TTL（分钟） */
    private static final long   ITEM_CACHE_TTL      = 30L;
    /** 空值缓存 TTL（分钟），防穿透 */
    private static final long   ITEM_CACHE_NULL_TTL = 2L;
    /** 随机 TTL 最大抖动（分钟），防雪崩 */
    private static final long   ITEM_CACHE_TTL_RAND = 10L;

    private final StringRedisTemplate redisTemplate;

    // ======================== 查询 ========================

    @Override
    public ItemDTO queryItemById(Long id) {
        String cacheKey = ITEM_CACHE_KEY + id;

        // 1. 查 Redis（命中含空值标记 → 防穿透）
        String json = redisTemplate.opsForValue().get(cacheKey);
        if (json != null) {
            return json.isEmpty() ? null : JSONUtil.toBean(json, ItemDTO.class);
        }

        // 2. 互斥锁防击穿：自旋等待，直到拿到锁或缓存被其他线程重建
        String lockKey = ITEM_LOCK_KEY + id;
        try {
            while (!tryLock(lockKey)) {
                Thread.sleep(50);
                // 自旋过程中若缓存已被重建，直接返回
                json = redisTemplate.opsForValue().get(cacheKey);
                if (json != null) {
                    return json.isEmpty() ? null : JSONUtil.toBean(json, ItemDTO.class);
                }
            }

            // 3. 拿到锁后双重检查（另一线程可能已完成重建）
            json = redisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                return json.isEmpty() ? null : JSONUtil.toBean(json, ItemDTO.class);
            }

            // 4. 查数据库
            Item item = getById(id);

            // 5. 写缓存
            if (item == null || Objects.equals(item.getStatus(), 3)) {
                // 数据不存在：缓存空值防穿透，短 TTL 避免长期占用
                redisTemplate.opsForValue().set(cacheKey, "", ITEM_CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            ItemDTO dto = BeanUtils.copyBean(item, ItemDTO.class);
            // 随机 TTL 防雪崩：base TTL + [0, rand) 分钟随机偏移
            long ttl = ITEM_CACHE_TTL + RandomUtil.randomLong(0, ITEM_CACHE_TTL_RAND);
            redisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(dto), ttl, TimeUnit.MINUTES);
            return dto;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizIllegalException("查询商品失败，请稍后重试");
        } finally {
            releaseLock(lockKey);
        }
    }

    @Override
    public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
        return BeanUtils.copyList(listByIds(ids), ItemDTO.class);
    }

    // ======================== 写操作（先写 DB，再删缓存） ========================

    @Override
    public void deductStock(List<OrderDetailDTO> items) {
        log.info("更新库存，商品列表：{}", items);
        String sqlStatement = "com.hmall.item.mapper.ItemMapper.updateStock";
        boolean r = false;
        try {
            r = executeBatch(items, (sqlSession, entity) -> sqlSession.update(sqlStatement, entity));
        } catch (Exception e) {
            throw new BizIllegalException("更新库存异常，可能是库存不足!", e);
        }
        if (!r) {
            throw new BizIllegalException("库存不足！");
        }
        // 库存变化后清除缓存，防止前端读到旧库存数据
        items.forEach(item -> evictItemCache(item.getItemId()));
    }

    @Override
    public boolean updateById(Item entity) {
        boolean result = super.updateById(entity);
        if (result && entity.getId() != null) {
            evictItemCache(entity.getId());
        }
        return result;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean result = super.removeById(id);
        if (result) {
            evictItemCache((Long) id);
        }
        return result;
    }

    // ======================== 私有工具 ========================

    /**
     * 尝试获取互斥锁（SETNX，10 秒自动释放防死锁）。
     */
    private boolean tryLock(String key) {
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(flag);
    }

    private void releaseLock(String key) {
        redisTemplate.delete(key);
    }

    private void evictItemCache(Long id) {
        redisTemplate.delete(ITEM_CACHE_KEY + id);
        log.debug("已清除商品缓存, id={}", id);
    }
}

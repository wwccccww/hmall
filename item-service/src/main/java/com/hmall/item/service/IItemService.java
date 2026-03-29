package com.hmall.item.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.item.domain.dto.ItemDTO;
import com.hmall.item.domain.dto.OrderDetailDTO;
import com.hmall.item.domain.po.Item;


import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 商品表 服务类
 * </p>
 */
public interface IItemService extends IService<Item> {

    void deductStock(List<OrderDetailDTO> items);

    List<ItemDTO> queryItemByIds(Collection<Long> ids);

    /**
     * 查询商品详情（带 Redis 缓存）。
     * 三种防护：
     *   1. 缓存空值  — 防穿透（DB 无数据时也写缓存）
     *   2. 互斥锁   — 防击穿（热点 key 过期时只有一个线程重建缓存）
     *   3. 随机 TTL — 防雪崩（缓存过期时间分散，避免同一时刻大量 key 失效）
     */
    ItemDTO queryItemById(Long id);
}

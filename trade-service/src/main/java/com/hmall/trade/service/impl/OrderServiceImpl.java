package com.hmall.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.client.CartClient;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.UserContext;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.mapper.OrderMapper;
import com.hmall.trade.service.IOrderDetailService;
import com.hmall.trade.service.IOrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final ItemClient itemClient;
    private final IOrderDetailService detailService;
    private final CartClient cartClient;

    @Override
    @GlobalTransactional
//    @Transactional
    public Long createOrder(OrderFormDTO orderFormDTO) {
        // 1.订单数据
        Order order = new Order();
        // 1.1.查询商品
        List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        // 1.2.获取商品id和数量的Map
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
        Set<Long> itemIds = itemNumMap.keySet();
        // 1.3.查询商品
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds);
        if (items == null || items.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }
        // 1.4.基于商品价格、购买数量计算商品总价：totalFee
        int total = 0;
        for (ItemDTO item : items) {
            total += item.getPrice()  * itemNumMap.getOrDefault(item.getId(),0);
        }
        order.setTotalFee(total);
        // 1.5.其它属性
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(UserContext.getUser());
        order.setStatus(1);
        // 1.6.将Order写入数据库order表中
        save(order);

        // 2.保存订单详情
        List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
        detailService.saveBatch(details);

        log.info("创建订单，订单id：{}", order.getId());
        for (OrderDetail detail : details) {
            log.info("订单详情：{}", detail);
        }
        // 3.扣减库存
        try {
            itemClient.deductStock(detailDTOS);
            for (OrderDetailDTO detail : detailDTOS) {
                log.info("扣减库存，商品id：{}, 数量：{}", detail.getItemId(), detail.getNum());
            }
        } catch (Exception e) {
            throw new RuntimeException("库存不足！");
        }

        // 4.清理购物车商品 (挪动到支付成功后再处理，防止未付钱就清空购物车)
        // cartClient.deleteCartItemByIds(itemIds);
        return order.getId();
    }

    @Override
    @Transactional
    public void markOrderPaySuccess(Long orderId) {
        // 1.查询订单
        Order order = getById(orderId);
        if (order == null || order.getStatus() != 1) {
            return;
        }

        // 2.修改订单状态
        order.setStatus(2); // 已支付
        updateById(order);

        // 3.清理购物车
        // 3.1.根据订单id查询订单详情，获取商品id集合
        List<OrderDetail> details = detailService.lambdaQuery().eq(OrderDetail::getOrderId, orderId).list();
        if (details == null || details.isEmpty()) {
            return;
        }
        Set<Long> itemIds = details.stream().map(OrderDetail::getItemId).collect(Collectors.toSet());
        // 3.2.删除购物车，注意：此处是后台线程，必须显式传递 userId
        cartClient.deleteCartItemByIds(itemIds, order.getUserId());
        for(Long itemId : itemIds){
            log.info("清理购物车，商品id：{}", itemId);
        }
    }

    /**
     * 构建订单详情
     * @param orderId 订单id
     * @param items 商品列表
     * @param numMap 商品id和数量的Map
     * @return 订单详情列表
     */
    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }
}
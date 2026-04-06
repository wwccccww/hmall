package com.hmall.trade.job;

import com.hmall.api.client.PayOrderClient;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付成功 MQ 未送达或消费失败时的补偿：轮询「长时间未付款」订单，向 pay 确认是否已支付，若已支付则补跑 markOrderPaySuccess。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaySuccessCompensationScheduler {

    private final IOrderService orderService;
    private final PayOrderClient payOrderClient;

    @Value("${hm.mq.compensation-order-min-age-minutes:2}")
    private int minAgeMinutes;
    @Value("${hm.mq.compensation-batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${hm.mq.compensation-interval-ms:120000}")
    public void reconcilePaidButPendingTradeOrders() {
        LocalDateTime before = LocalDateTime.now().minusMinutes(minAgeMinutes);
        List<Order> candidates = orderService.lambdaQuery()
                .eq(Order::getStatus, 1)
                .lt(Order::getCreateTime, before)
                .orderByAsc(Order::getCreateTime)
                .last("LIMIT " + batchSize)
                .list();
        if (candidates.isEmpty()) {
            return;
        }
        for (Order o : candidates) {
            try {
                Boolean paid = payOrderClient.isBizOrderPaid(o.getId());
                if (Boolean.TRUE.equals(paid)) {
                    log.warn("MQ 补偿：支付侧已成功，补执行订单履约 orderId={}", o.getId());
                    orderService.markOrderPaySuccess(o.getId());
                }
            } catch (Exception e) {
                log.error("MQ 补偿对账调用失败 orderId={}", o.getId(), e);
            }
        }
    }
}

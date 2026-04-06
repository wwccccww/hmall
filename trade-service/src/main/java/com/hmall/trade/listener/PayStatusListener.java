package com.hmall.trade.listener;

import com.hmall.trade.service.IOrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.hmall.trade.config.TradeRabbitMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class PayStatusListener {
    private final IOrderService orderService;
    @RabbitListener(queues = TradeRabbitMqConfig.PAY_QUEUE)
    public void listenPaySuccess(Long orderId) {
        log.info("PayStatusListener 监听支付成功消息:{}", orderId);
        // 异常向上抛出：由容器 retry + default-requeue-rejected=false 触发 reject，消息进入 DLX/DLQ，避免静默 ACK 丢单
        orderService.markOrderPaySuccess(orderId);
    }
}

package com.hmall.trade.listener;

import com.hmall.trade.service.IOrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class PayStatusListener {
    private final IOrderService orderService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "trade.pay.success.queue", durable = "true"),
            exchange = @Exchange(name="pay.direct"),
            key = "pay.success"
    ))
    public void listenPaySuccess(Long orderId) {
        log.info("PayStatusListener 监听支付成功消息:{}", orderId);
        try {
            orderService.markOrderPaySuccess(orderId);
        } catch (Exception e) {
            // 防止 RabbitListener 因异常反复重试导致控制台刷屏
            log.error("处理支付成功失败，orderId={}", orderId, e);
        }
    }
}

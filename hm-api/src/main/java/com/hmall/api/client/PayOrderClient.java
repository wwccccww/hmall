package com.hmall.api.client;

import com.hmall.api.config.DefaultFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 支付单查询（订单补偿对账：trade 调用 pay）。
 */
@FeignClient(value = "pay-service", configuration = DefaultFeignConfig.class)
public interface PayOrderClient {

    /**
     * @param bizOrderNo 业务订单号（与 trade 订单 id 一致）
     * @return true 表示支付已成功
     */
    @GetMapping("/pay-orders/internal/paid/{bizOrderNo}")
    Boolean isBizOrderPaid(@PathVariable("bizOrderNo") Long bizOrderNo);
}

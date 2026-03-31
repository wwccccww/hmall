package com.hmall.api.client;

import com.hmall.api.dto.promotion.CouponPreviewRequest;
import com.hmall.api.dto.promotion.CouponPreviewVO;
import com.hmall.api.dto.promotion.CouponRedeemRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("promotion-service")
public interface PromotionClient {

    @PostMapping("/coupons/preview")
    CouponPreviewVO previewCoupon(@RequestBody CouponPreviewRequest request);

    @PostMapping("/coupons/redeem")
    void redeemCoupon(@RequestBody CouponRedeemRequest request);
}


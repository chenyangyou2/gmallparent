package com.atguigu.gmall.payment.client;


import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.client.impl.PaymentDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(value = "service-payment",fallback = PaymentDegradeFeignClient.class)
public interface PaymentFeignClient {

    /**
     * 关闭支付宝交易记录控制器
     */
    @GetMapping("/api/payment/alipay/closePay/{orderId}")
    @ResponseBody
    Boolean closePay(@PathVariable Long orderId);

    /**
     * 查询交易记录接口
     */
    @GetMapping("/api/payment/alipay/checkPayment/{orderId}")
    @ResponseBody
    Boolean checkPayment(@PathVariable Long orderId);

    /**
     * 远程调用查询paymentInfo 记录的接口
     */
    @GetMapping("/api/payment/alipay/getPaymentInfo/{outTradeNo}")
    @ResponseBody
    PaymentInfo getPaymentInfo(@PathVariable String outTradeNo);
}

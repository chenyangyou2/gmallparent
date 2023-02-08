package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

public interface AliPayService {
    /**
     * 根据订单Id生成二维码
     */
    String crateAliPay(Long orderId) throws AlipayApiException;

    /**
     * 退款
     */
    Boolean refund(Long orderId);

    /**
     * 关闭支付宝交易记录控制器
     */
    Boolean closePay(Long orderId);

    /**
     * 查询交易记录接口
     */
    Boolean checkPayment(Long orderId);
}

package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

public interface PaymentService {


    /**
     * 保存支付交易记录
     */
    void savePaymentInfo(OrderInfo orderInfo , String paymentType);

    /**
     * 获取PaymentInfo 对象
     */
    PaymentInfo getPaymentInfo(String outTradeNo, String paymentType);

    /**
     * 更新交易记录状态
     */
    void paySuccess(String outTradeNo, String paymentType, Map<String, String> paramsMap);

    /**
     * 更新条件
     */
    void updatePaymentInfoStatus(String outTradeNo, String paymentType, PaymentInfo paymentInfo);

    /**
     * 关闭本地交易记录
     */
    void closePayment(Long orderId);
}

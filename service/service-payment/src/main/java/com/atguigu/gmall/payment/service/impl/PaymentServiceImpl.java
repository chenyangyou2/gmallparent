//package com.atguigu.gmall.payment.service.impl;
//
//import com.atguigu.gmall.model.enums.PaymentStatus;
//import com.atguigu.gmall.model.order.OrderInfo;
//import com.atguigu.gmall.model.payment.PaymentInfo;
//import com.atguigu.gmall.mq.Service.RabbitService;
//import com.atguigu.gmall.mq.constant.MqConst;
//import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
//import com.atguigu.gmall.payment.service.PaymentService;
//import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.Date;
//import java.util.Map;
//
//@Service
//public class PaymentServiceImpl implements PaymentService {
//
//    @Autowired
//    private PaymentInfoMapper paymentInfoMapper;
//
//    @Autowired
//    private RedisTemplate redisTemplate;
//
//    @Autowired
//    private RabbitService rabbitService;
//
//    @Override
//    /**
//     * 保存支付交易记录
//     */
//    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
//        // 一种订单对应的同一种支付方式 在表中只能出现一次
//        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
//        paymentInfoQueryWrapper.eq("order_id" , orderInfo.getId());
//        paymentInfoQueryWrapper.eq("payment_type" , paymentType);
//        PaymentInfo paymentInfoQuery = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
//        if (paymentInfoQuery != null){
//            return;
//        }
//
//
//        // 创一个paymentInfo
//        PaymentInfo paymentInfo = new PaymentInfo();
//        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
//        paymentInfo.setOrderId(orderInfo.getId());
//        paymentInfo.setUserId(orderInfo.getUserId());
//        paymentInfo.setPaymentType(paymentType);
//        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
//        paymentInfo.setSubject(orderInfo.getTradeBody());
//        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
//        paymentInfoMapper.insert(paymentInfo);
//    }
//
//    @Override
//    /**
//     * 获取PaymentInfo 对象
//     */
//    public PaymentInfo getPaymentInfo(String outTradeNo, String paymentType) {
//        //通过outTradeNo，支付类型查询
//        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
//        paymentInfoQueryWrapper.eq("out_trade_no" , outTradeNo);
//        paymentInfoQueryWrapper.eq("payment_type" , paymentType);
//        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
//        if (paymentInfo != null){
//            return paymentInfo;
//        }
//        return null;
//    }
//
//    @Override
//    /**
//     * 更新交易记录状态
//     */
//    public void paySuccess(String outTradeNo, String paymentType, Map<String, String> paramsMap) {
//        //
//        try {
//            PaymentInfo paymentInfoQuery = getPaymentInfo(outTradeNo, paymentType);
//            if ("PAID".equals(paymentInfoQuery.getPaymentStatus())) {
//
//                return;
//            }
//
//
//            PaymentInfo paymentInfo = new PaymentInfo();
//            paymentInfo.setTradeNo(paramsMap.get("trade_no"));
//            paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
//            paymentInfo.setCallbackTime(new Date());
//            paymentInfo.setCallbackContent(paramsMap.toString());
//            paymentInfo.setUpdateTime(new Date());
//
//            // 构建更新条件
////            QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
////            paymentInfoQueryWrapper.eq("out_trade_no" , outTradeNo);
////            paymentInfoQueryWrapper.eq("payment_type" , paymentType);
////            paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);
//            //更新交易记录
//            updatePaymentInfoStatus(outTradeNo , paymentType ,paymentInfo);
//            //发送消息个订单
//            rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfoQuery.getOrderId());
//        } catch (Exception e) {
//            //
//            redisTemplate.delete(paramsMap.get("notify_id"));
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void updatePaymentInfoStatus(String outTradeNo, String paymentType, PaymentInfo paymentInfo) {
//        // 调用mapper 的更新方法
//        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
//        paymentInfoQueryWrapper.eq("out_trade_no" , outTradeNo);
//        paymentInfoQueryWrapper.eq("payment_type" , paymentType);
//        paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);
//    }
//}
//
//
//
//
//
//
//
//
//






















package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.mq.Service.RabbitService;
import com.atguigu.gmall.mq.constant.MqConst;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * author:atGuiGu-mqx
 * date:2022/7/1 9:46
 * 描述：
 **/
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;


    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        //  细节：一个订单对应的同一种支付方式 在表中只能出现一次！
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id",orderInfo.getId());
        paymentInfoQueryWrapper.eq("payment_type",paymentType);
        PaymentInfo paymentInfoQuery = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        if (paymentInfoQuery!=null){
            return;
        }

        //  创建一个paymentInfo
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfoMapper.insert(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String paymentType) {
        //  通过outTradeNo, 支付类型查询
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo);
        paymentInfoQueryWrapper.eq("payment_type",paymentType);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        //  判断
        if (paymentInfo!=null){
            return paymentInfo;
        }
        return null;
    }

    @Override
    public void paySuccess(String outTradeNo, String paymentType, Map<String, String> paramsMap) {
        //  更新的时候，判断一下 当前的交易记录状态如果是PAID,
        try {
            PaymentInfo paymentInfoQuery = this.getPaymentInfo(outTradeNo, paymentType);
            if ("PAID".equals(paymentInfoQuery.getPaymentStatus())){
                return;
            }

            //  更新 trade_no payment_status callback_time callback_content
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setTradeNo(paramsMap.get("trade_no"));
            paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
            paymentInfo.setCallbackTime(new Date());
            paymentInfo.setCallbackContent(paramsMap.toString());
            paymentInfo.setUpdateTime(new Date());
            //  构建更新条件
            //            QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
            //            paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo);
            //            paymentInfoQueryWrapper.eq("payment_type",paymentType);
            //  this.paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);

            //  更新交易记录。
            this.updatePaymentInfoStatus(outTradeNo,paymentType,paymentInfo);

            // 发送消息给订单
            rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfoQuery.getOrderId());

        } catch (Exception e) {
            //  如果执行错误了,得让第二次通知进来.
            redisTemplate.delete(paramsMap.get("notify_id"));
            e.printStackTrace();
        }
    }

    @Override
    public void updatePaymentInfoStatus(String outTradeNo, String paymentType, PaymentInfo paymentInfo) {
        //  调用mapper 的更新方法
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo);
        paymentInfoQueryWrapper.eq("payment_type",paymentType);
        this.paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);
    }

    /**
     * 关闭本地交易记录
     */
    public void closePayment(Long orderId) {
        //  更新paymentInfo 的支付状态
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
        paymentInfo.setUpdateTime(new Date());
        //  设置更新条件
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id",orderId);
        this.paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);

    }
}
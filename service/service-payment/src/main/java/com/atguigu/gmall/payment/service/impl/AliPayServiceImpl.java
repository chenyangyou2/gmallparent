//package com.atguigu.gmall.payment.service.impl;
//
//        import com.alibaba.fastjson.JSONObject;
//        import com.alipay.api.AlipayApiException;
//        import com.alipay.api.AlipayClient;
//        import com.alipay.api.request.AlipayTradePagePayRequest;
//        import com.alipay.api.request.AlipayTradeRefundRequest;
//        import com.alipay.api.response.AlipayTradeRefundResponse;
//        import com.atguigu.gmall.model.enums.PaymentStatus;
//        import com.atguigu.gmall.model.enums.PaymentType;
//        import com.atguigu.gmall.model.order.OrderInfo;
//        import com.atguigu.gmall.model.payment.PaymentInfo;
//        import com.atguigu.gmall.order.client.OrderFeignClient;
//        import com.atguigu.gmall.payment.config.AlipayConfig;
//        import com.atguigu.gmall.payment.service.AliPayService;
//        import com.atguigu.gmall.payment.service.PaymentService;
//        import lombok.SneakyThrows;
//        import org.springframework.beans.factory.annotation.Autowired;
//        import org.springframework.stereotype.Service;
//
//        import java.text.SimpleDateFormat;
//        import java.util.Calendar;
//        import java.util.Date;
//
//@Service
//public class AlipayServiceImpl implements AliPayService {
//
//    @Autowired
//    private AlipayClient alipayClient;
//
//    @Autowired
//    private OrderFeignClient orderFeignClient;
//
//    @Autowired
//    private PaymentService paymentService;
//
//    @SneakyThrows
//    @Override
//    public String crateAliPay(Long orderId) {
//        // 根据订单orderId 获取orderInfo
//        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
//
//        // 如果订单关闭，或者已经支付过了，需要生成二维码吗？
//        if ("CLOSED".equals(orderInfo.getOrderStatus()) || "PAID".equals(orderInfo.getOrderStatus())){
//            return "订单支付或者关闭";
//        }
//
//        //调用保存订单
//        paymentService.savePaymentInfo(orderInfo , PaymentType.ALIPAY.name());
//
////        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
//        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
//        //设置异步回调地址
//        request.setNotifyUrl(AlipayConfig.notify_payment_url);
//        //设置异步回调地址
//        request.setReturnUrl(AlipayConfig.return_payment_url);
//        JSONObject bizContent = new JSONObject();
//        //赋值订单编号
//        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
//        //给固定的0.01
//        bizContent.put("total_amount", 0.01);
//        bizContent.put("subject", orderInfo.getTradeBody());
//        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
//
//        // 订单超时时间
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        // 设置时间 10m 不支付就关闭
//        Calendar calendar = Calendar.getInstance();
//        calendar.add(Calendar.MINUTE , 10);
//        bizContent.put("time_expire", simpleDateFormat.format(calendar.getTime()));
//
////        // 商品明细信息，按需传入
////        JSONArray goodsDetail = new JSONArray();
////        JSONObject goods1 = new JSONObject();
////        goods1.put("goods_id", "goodsNo1");
////        goods1.put("goods_name", "子商品1");
////        goods1.put("quantity", 1);
////        goods1.put("price", 0.01);
////        goodsDetail.add(goods1);
////        bizContent.put("goods_detail", goodsDetail);
//
//        //// 扩展信息，按需传入
//        //JSONObject extendParams = new JSONObject();
//        //extendParams.put("sys_service_provider_id", "2088511833207846");
//        //bizContent.put("extend_params", extendParams);
//
//        request.setBizContent(bizContent.toString());
//        String from = alipayClient.pageExecute(request).getBody();
//
//        return from;
//    }
//
//    @Override
//    public Boolean refund(Long orderId) {
//        //  根据订单Id 获取orderInfo数据
//        OrderInfo orderInfo = this.orderFeignClient.getOrderInfo(orderId);
//
//        //  AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
//        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
//        JSONObject bizContent = new JSONObject();
//        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
//        // bizContent.put("refund_amount", orderInfo.getTotalAmount());
//        bizContent.put("refund_amount", 0.01);
//        bizContent.put("out_request_no", "HZ01RF001");
//
//        request.setBizContent(bizContent.toString());
//        AlipayTradeRefundResponse response = null;
//        try {
//            response = alipayClient.execute(request);
//        } catch (AlipayApiException e) {
//            e.printStackTrace();
//        }
//        if(response.isSuccess()){
//            System.out.println("退款成功");
//            //  如果退款成功，则将交易记录状态修改为 CLOSED.
//            //  更新条件，还有更新的内容，传递进去
//            PaymentInfo paymentInfo = new PaymentInfo();
//            paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
//            paymentInfo.setUpdateTime(new Date());
//            //  调用更新方法
//            paymentService.updatePaymentInfoStatus(orderInfo.getOutTradeNo(),PaymentType.ALIPAY.name(),paymentInfo);
//            return true;
//        } else {
//            System.out.println("退款失败");
//            return false;
//        }
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
//
//
//
//
//
//












package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AliPayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * author:atGuiGu-mqx
 * date:2022/7/1 10:33
 * 描述：
 **/
@Service
public class AliPayServiceImpl implements AliPayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private PaymentService paymentService;

    @Override
    public String crateAliPay(Long orderId) throws AlipayApiException {
        //  根据订单orderId 获取到orderInfo
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);

        //  如果订单关闭了，或者已经支付过了，还需要生成二维码么！ no!
        if ("CLOSED".equals(orderInfo.getOrderStatus()) || "PAID".equals(orderInfo.getOrderStatus())){
            //  返回信息提示.
            return "订单支付或已关闭";
        }

        //  调用保存订单.
        paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());

        //  AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        //  设置异步回调地址
        request.setNotifyUrl(AlipayConfig.notify_payment_url);
        //  设置同步回调地址
        request.setReturnUrl(AlipayConfig.return_payment_url);
        JSONObject bizContent = new JSONObject();
        //  赋值商品订单号
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        //  固定给0.01
        bizContent.put("total_amount", 0.01);
        bizContent.put("subject", orderInfo.getTradeBody());
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        //  订单绝对超时时间。
        //  格式为yyyy-MM-dd HH:mm:ss
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //  设置一个绝对时间 10m 不支付就关闭.
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE,10);
        bizContent.put("time_expire", simpleDateFormat.format(calendar.getTime()));

        request.setBizContent(bizContent.toString());
        //  调用方法生成表单
        String from = alipayClient.pageExecute(request).getBody();
        //  将表单返回去
        return from;
    }

    @Override
    public Boolean refund(Long orderId) {
        //  根据订单Id 获取orderInfo数据
        OrderInfo orderInfo = this.orderFeignClient.getOrderInfo(orderId);

        //  AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        // bizContent.put("refund_amount", orderInfo.getTotalAmount());
        bizContent.put("refund_amount", 0.01);
        bizContent.put("out_request_no", "HZ01RF001");

        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("退款成功");
            //  如果退款成功，则将交易记录状态修改为 CLOSED.
            //  更新条件，还有更新的内容，传递进去
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
            paymentInfo.setUpdateTime(new Date());
            //  调用更新方法
            paymentService.updatePaymentInfoStatus(orderInfo.getOutTradeNo(),PaymentType.ALIPAY.name(),paymentInfo);
            return true;
        } else {
            System.out.println("退款失败");
            return false;
        }
    }

    /**
     * 关闭支付宝交易记录控制器
     */
    @Override
    public Boolean closePay(Long orderId) {
        //  根据订单Id 获取到orderInfo 对象。
        OrderInfo orderInfo = this.orderFeignClient.getOrderInfo(orderId);

        //  关闭支付宝交易记录
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        JSONObject bizContent = new JSONObject();
        // trade_no  或  out_trade_no 二选一。
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeCloseResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    @Override
    /**
     * 查询交易记录接口
     */
    public Boolean checkPayment(Long orderId) {
        //  根据订单Id 获取到 orderInfo 对象
        OrderInfo orderInfo = this.orderFeignClient.getOrderInfo(orderId);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no",orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }
}
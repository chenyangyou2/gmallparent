//package com.atguigu.gmall.payment.controller;
//
//import com.alipay.api.AlipayApiException;
//import com.alipay.api.internal.util.AlipaySignature;
//import com.atguigu.gmall.model.enums.PaymentType;
//import com.atguigu.gmall.model.payment.PaymentInfo;
//import com.atguigu.gmall.payment.config.AlipayConfig;
//import com.atguigu.gmall.payment.service.AlipayService;
//import com.atguigu.gmall.payment.service.PaymentService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.*;
//
//import java.math.BigDecimal;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//
////@RestController
//@Controller
//@RequestMapping("/api/payment/alipay")
//public class AliPayController {
//    @Autowired
//    private AlipayService alipayService;
//
//    @Autowired
//    private PaymentService paymentService;
//
//    @Autowired
//    private RedisTemplate redisTemplate;
//
//    @Value("${app_id}")
//    private String app_id;
//
//
//    /**
//     * 生成二维码
//     * http://api.gmall.com/api/payment/alipay/submit/61
//     */
//    @GetMapping("submit/{orderId}")
//    @ResponseBody
//    public String submitPay(@PathVariable Long orderId) {
//        String from = null;
//        try {
//            from = alipayService.crateAliPay(orderId);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return from;
//    }
//
//    /**
//     * 同步回调
//     * http://api.gmall.com/api/payment/alipay/callback/return
//     */
//    @GetMapping("callback/return")
//    public String callbackReturn() {
//
//        //http://payment.gmall.com/pay/success.html
//        return "redirect:" + AlipayConfig.return_order_url;
//    }
//
//    // 异步回调
//    @PostMapping("callback/notify")
//    @ResponseBody
//    public String callbackNotify(@RequestParam Map<String, String> paramsMap) {
//        System.out.println("异步回调通知————————");
//        boolean signVerified = false; //调用SDK验证签名
//        try {
//            signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
//        } catch (AlipayApiException e) {
//            e.printStackTrace();
//        }
//        // 获取异步通知中的参数
//        String outTradeNo = paramsMap.get("out_trade_no");
//        String totalAmount = paramsMap.get("total_amount");
//        String appId = paramsMap.get("app_id");
//
//        String tradeStatus = paramsMap.get("trade_status");
//        String notifyId = paramsMap.get("notify_id");
//        if (signVerified) {
//            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
//            // 通过outTradeNo 查询 paymentInfo
//            PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
//            if (paymentInfoQuery == null || new BigDecimal("0.01").compareTo(new BigDecimal(totalAmount)) != 0
//                    || !appId.equals(app_id)) {
//                return "failure";
//            }
//            //只要支付宝服务器没有收到 success 这七个字符，则会在24小时22分钟内发送8次通知。使用notify_id进行过滤
//            Boolean result = redisTemplate.opsForValue().setIfAbsent(notifyId, notifyId, 1452 * 60, TimeUnit.SECONDS);
//            // key 不存在返回true
//            if (!result) {
//                return "failure";
//            }
//            //判断支付
//            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
//                // 更新交易记录状态：
//                paymentService.paySuccess(outTradeNo, PaymentType.ALIPAY.name(), paramsMap);
//                return "success";
//            }
//        } else {
//            // TODO 验签失败则记录异常日志，并在response中返回failure.
//            return "failure";
//        }
//        return "failure";
//    }
//
//    /**
//     * 退款接口
//     * orderId 查询 ---》 out_trade_on 商户订单
//     */
//    @GetMapping("refund/{orderId}")
//    @ResponseBody
//    public Boolean refund(@PathVariable Long orderId){
//        //退款方法
//        Boolean flag = alipayService.refund(orderId);
//        return flag;
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
//
package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AliPayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;


// @RestController // @ResponseBody + @Controller
@Controller
@RequestMapping("/api/payment/alipay")
public class AliPayController {

    @Autowired
    private AliPayService aliPayService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${app_id}")
    private String app_id;



    //  生成二维码
    //  http://api.gmall.com/api/payment/alipay/submit/47
    @GetMapping("submit/{orderId}")
    @ResponseBody
    public String submitPay(@PathVariable Long orderId){
        String from = null;
        try {
            from = aliPayService.crateAliPay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return from;
    }

    //  同步回调 http://api.gmall.com/api/payment/alipay/callback/return
    @GetMapping("callback/return")
    public String callbackReturn(){
        //  更新订单处理.
        //   http://payment.gmall.com/pay/success.html
        return "redirect:"+ AlipayConfig.return_order_url;
    }

    //  异步回调
    //  http://rjsh38.natappfree.cc/api/payment/alipay/callback/notify
    //  https: //商家网站通知地址?voucher_detail_list=[{"amount":"0.20","merchantContribute":"0.00","name":"5折券","otherContribute":"0.20","type":"ALIPAY_DISCOUNT_VOUCHER","voucherId":"2016101200073002586200003BQ4"}]&fund_bill_list=[{"amount":"0.80","fundChannel":"ALIPAYACCOUNT"},{"amount":"0.20","fundChannel":"MDISCOUNT"}]&subject=PC网站支付交易&trade_no=2016101221001004580200203978&gmt_create=2016-10-12 21:36:12&notify_type=trade_status_sync&total_amount=1.00&out_trade_no=mobile_rdm862016-10-12213600&invoice_amount=0.80&seller_id=2088201909970555&notify_time=2016-10-12 21:41:23&trade_status=TRADE_SUCCESS&gmt_payment=2016-10-12 21:37:19&receipt_amount=0.80&passback_params=passback_params123&buyer_id=2088102114562585&app_id=2016092101248425&notify_id=7676a2e1e4e737cff30015c4b7b55e3kh6& sign_type=RSA2&buyer_pay_amount=0.80&sign=***&point_amount=0.00
    @PostMapping("callback/notify")
    @ResponseBody
    public String callbackNotify(@RequestParam Map<String, String> paramsMap){
        System.out.println("异步回调通知....");
        //  Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        boolean signVerified = false; //调用SDK验证签名
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //  获取异步通知中的参数
        String outTradeNo = paramsMap.get("out_trade_no");
        String totalAmount = paramsMap.get("total_amount");
        String appId = paramsMap.get("app_id");

        String tradeStatus = paramsMap.get("trade_status");
        String notifyId = paramsMap.get("notify_id");
        //  通知的参数与系统中的参数是否一致！

        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            //  通过outTradeNo 查询 paymentInfo
            PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
            //  paymentInfoQuery.getTotalAmount().compareTo(new BigDecimal(totalAmount))!=0 正常业务中的判断.
            //  我们支付的时候给的是0.01 因此使用0.01 判断
            if (paymentInfoQuery==null || new BigDecimal("0.01").compareTo(new BigDecimal(totalAmount))!=0
                    || !appId.equals(app_id)){
                return "failure";
            }
            //  只要支付宝服务器没有接收到 success 这七个字符，则会在24小时22分钟内发送8次通知。 使用notify_id 进行过滤。
            Boolean result = this.redisTemplate.opsForValue().setIfAbsent(notifyId, notifyId, 1462 * 60, TimeUnit.SECONDS);
            // key 不存在返回true
            if (!result){
                // 说明缓存中有数据
                return "failure";
            }
            //  判断支付状态
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)){
                //  更新交易记录状态.trade_no payment_status callback_time callback_content
                //  paramsMap 目的是 给 trade_no 赋值，同时将这个map 给 callback_content
                paymentService.paySuccess(outTradeNo, PaymentType.ALIPAY.name(),paramsMap);
                return "success";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    //  编写退款接口：
    //  orderId 查询---> out_trade_no 商户订单号
    @GetMapping("refund/{orderId}")
    @ResponseBody
    public Boolean refund(@PathVariable Long orderId){
        //  退款方法.
        Boolean flag = this.aliPayService.refund(orderId);

        return flag;
    }

    /**
     * 关闭支付宝交易记录控制器
     */
    @GetMapping("closePay/{orderId}")
    @ResponseBody
    public Boolean closePay(@PathVariable Long orderId){
        //  调用服务层方法  paymentService -- 通常管理电商本地的服务对象
        //  Boolean flag = this.paymentService.closePay(orderId);

        //  关闭支付宝的交易记录。aliPayService -- 管理支付宝的服务对象 aliPayService.closePay(orderId);
        Boolean flag = this.aliPayService.closePay(orderId);
        return flag;
    }

    /**
     * 查询交易记录接口
     */
    @GetMapping("checkPayment/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId){
        //  调用服务层方法
        Boolean flag = this.aliPayService.checkPayment(orderId);
        //  返回数据
        return flag;
    }

    /**
     * 远程调用查询paymentInfo 记录的接口
     */
    @GetMapping("getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo){
        //  调用服务层方法
        PaymentInfo paymentInfo = this.paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        if (paymentInfo!=null){
            return paymentInfo;
        }
        return null;

    }
}
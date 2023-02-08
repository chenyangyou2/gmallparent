package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class PaymentController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    @GetMapping("pay.html")
    public String pay(HttpServletRequest request){
        // 获取订单Id
        String orderId = request.getParameter("orderId");
        //调用远程方法
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(Long.parseLong(orderId));
        //保存数据
        request.setAttribute("orderInfo" , orderInfo);

        return "payment/pay";

    }

    /**
     * 同步回调
     * pay/success.html
     */
    @GetMapping("pay/success.html")
    public String paySuccess(){
        return "payment/success";
    }
}

































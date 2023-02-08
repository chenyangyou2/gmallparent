package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class OrderController {

    //  远程调用OrderFeignClient
    @Autowired
    private OrderFeignClient orderFeignClient;

    //  http://order.gmall.com/trade.html
    @GetMapping("trade.html")
    public String trade(Model model){
        //  后台要存储  userAddressList，detailArrayList，totalNum，totalAmount 这些数据都是从 service-order 中获取的！
        //  service-order 中以map 的形式存储数据.
        Result<Map> result = orderFeignClient.trade();
        model.addAllAttributes(result.getData());
        //  返回订单结算页
        return "order/trade";
    }
    /**
     * 查看我的订单
     * http://order.gmall.com/myOrder.html
     */
    @GetMapping("myOrder.html")
    public String myOrder(){
        return "order/myOrder";
    }
}


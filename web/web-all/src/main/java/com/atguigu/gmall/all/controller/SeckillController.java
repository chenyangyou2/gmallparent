package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class SeckillController {

    @Autowired
    private ActivityFeignClient activityFeignClient;

    /**
     * 去秒杀页面
     * http://activity.gmall.com/seckill.html
     */
    @GetMapping("seckill.html")
    public String seckillList(Model model){
        //后台存储一个list 集合数据
        Result result = activityFeignClient.findAll();
        model.addAttribute("list" ,result.getData());
        return "seckill/index";
    }

    /**
     * 立即抢购
     * th:href=" '/seckill/' + ${item.skuId} + '.html' "
     */
    @GetMapping("/seckill/{skuId}.html")
    public String seckillItem(@PathVariable Long skuId ,Model model){
        //获取商品详情
        Result result = activityFeignClient.getSeckillGoods(skuId);
        model.addAttribute("item" , result.getData());
        return "seckill/item";
    }
}

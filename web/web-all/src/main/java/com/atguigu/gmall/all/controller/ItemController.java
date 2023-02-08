package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Controller  // 注意不要写RestController
public class ItemController {

    @Autowired
    private ItemFeignClient itemFeignClient;

    //  http://item.gmall.com/23.html
    @GetMapping("{skuId}.html")
    public String skuItem(@PathVariable Long skuId, Model model){
        //  通过skuId 实现远程调用！ 页面需要使用的数据都保存到哪了?  在result.getData(); 中！
        Result<Map> result = itemFeignClient.skuIdItem(skuId);
        //  存储单个值
        //  model.addAttribute("name","刘德华");
        //  存储的对象是Map 集合 ，并非单个值
        model.addAllAttributes(result.getData());
        //  返回页面 spring boot 默认找视图名称的地址：
        return "item/item";
    }
}
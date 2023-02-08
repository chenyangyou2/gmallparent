package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/item")
public class ItemApiController {

    @Autowired
    private ItemService itemService;

    /**
     * 编写一个给web—all提供数据的控制地址
     * /api/item/{skuId}
     */
    @GetMapping("{skuId}")
    public Result skuIdItem(@PathVariable Long skuId){
        //
        Map map = itemService.getItem(skuId);
        return Result.ok(map);
    }
}

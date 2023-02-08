package com.atguigu.gmall.activity.client;

import com.atguigu.gmall.activity.client.impl.ActivityDegradeFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "service-activity" , fallback = ActivityDegradeFeignClient.class)
public interface ActivityFeignClient {

    /**
     * 获取全部数据
     */
    @GetMapping("/api/activity/seckill/findAll")
    Result findAll();

    /**
     * 点击立即抢购获取到秒杀详情
     */
    @GetMapping("/api/activity/seckill/getSeckillGoods/{skuId}")
    Result getSeckillGoods(@PathVariable Long skuId);
}

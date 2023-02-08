package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

public interface SeckillGoodsService {
    /**
     * 获取全部数据
     */
    List<SeckillGoods> findAll();

    /**
     * 点击立即抢购获取到秒杀详情
     */
    SeckillGoods getSeckillGoods(Long skuId);
}

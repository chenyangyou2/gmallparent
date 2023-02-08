package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    //  查询秒杀列表，不需要查询数据库. 直接查询缓存就可以了.
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    /**
     * 获取全部数据
     */
    public List<SeckillGoods> findAll() {
        //  key = seckill:goods  hvals key
        List<SeckillGoods> seckillGoodsList = this.redisTemplate.opsForHash().values(RedisConst.SECKILL_GOODS);
        //  返回数据
        return seckillGoodsList;
    }

    @Override
    public SeckillGoods getSeckillGoods(Long skuId) {
        //  hget key field; 注意： field 数据类型是String 因此在获取的时候，要将 skuId 变为字符串。
        SeckillGoods seckillGoods = (SeckillGoods) this.redisTemplate.opsForHash().get(RedisConst.SECKILL_GOODS,skuId.toString());
        //  返回数据
        return seckillGoods;
    }


}

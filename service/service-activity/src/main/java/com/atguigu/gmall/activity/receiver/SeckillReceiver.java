package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.mq.constant.MqConst;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;


@Component
public class SeckillReceiver {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;


    //  监听定时任务发送的消息.
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void getData(String msg, Message message, Channel channel){
        //  将数据导入redis.
        //  条件1 必须是当天要秒杀的商品， 条件2 商品的剩余库存数>0， 条件3 必须是审核通过的商品
        //  select * from seckill_goods where date_format(start_time,'%Y-%m-%d') = (select date_format(now(),'%Y-%m-%d') from dual);
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        seckillGoodsQueryWrapper.eq("date_format(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        seckillGoodsQueryWrapper.gt("stock_count",0);
        seckillGoodsQueryWrapper.eq("status","1");
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);
        //  循环遍历集合将数据放入缓存
        if (!CollectionUtils.isEmpty(seckillGoodsList)){
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                //  将商品存储到缓存，存储到缓存，使用哪种数据类型： hash 数据类型。
                //  hset key field value  hget key field hvals
                String secKillKey = RedisConst.SECKILL_GOODS;
                //  判断当前缓存中如果存在这个秒杀商品，就不能覆盖写.
                Boolean exist = redisTemplate.opsForHash().hasKey(secKillKey, seckillGoods.getSkuId().toString());
                if (exist){
                    //  如果存在，则不执行后续的添加.
                    continue;  // return; break;
                }
                //  存储数据
                redisTemplate.opsForHash().put(secKillKey,seckillGoods.getSkuId().toString(),seckillGoods);

                //  将商品的剩余库存数量，放入 redis - list 队列中.  stock_count = 10
                for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {
                    //  定义key = seckill:stock:skuId
                    String stockKey = RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId();
                    //  rpush key skuId  lpop key
                    this.redisTemplate.opsForList().rightPush(stockKey,seckillGoods.getSkuId().toString());
                }
                //  在商品存储缓存的时候，将所有skuId对应的状态位都设置为 可以秒杀：  skuId:1 可以秒杀 skuId:0 不可以秒杀。
                //  publish seckillpush  24:1
                this.redisTemplate.convertAndSend("seckillpush",seckillGoods.getSkuId()+":1");
            }
        }
    }

    // 消息监听
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1 , durable = "true" , autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void getMsg(UserRecode userRecode , Message message , Channel channel){

    }
}
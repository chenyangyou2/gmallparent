package com.atguigu.gmall.mq.Service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.mq.model.GmallCorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import springfox.documentation.annotations.ApiIgnore;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    //分装一个发送消息的方法
    public Boolean sendMsg(String exchange , String routingKey , Object msg){
        //将发送消息赋值到自定义的实体类
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        //声明一个correlationId的变量
        String correlationId = UUID.randomUUID().toString().replaceAll("-","");
        gmallCorrelationData.setId(correlationId);
        gmallCorrelationData.setExchange(exchange);
        gmallCorrelationData.setRoutingKey(routingKey);
        gmallCorrelationData.setMessage(msg);

        //发送消息大的时候，将这个gmallCorrelationData 对象放入缓存
        redisTemplate.opsForValue().set(correlationId , JSON.toJSONString(gmallCorrelationData) , 10 , TimeUnit.MINUTES);

        //调用发送消息方法
//        rabbitTemplate.convertAndSend(exchange , routingKey , msg);
        rabbitTemplate.convertAndSend(exchange,routingKey,msg,gmallCorrelationData);
        return true;
    }

    public Boolean sendDelayMsg(String exchange , String routingKey , Object msg , int delayTime){
        //将发送消息赋值到自定义的实体类
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        //声明一个correlationId的变量
        String correlationId = UUID.randomUUID().toString().replaceAll("-","");
        gmallCorrelationData.setId(correlationId);
        gmallCorrelationData.setExchange(exchange);
        gmallCorrelationData.setRoutingKey(routingKey);
        gmallCorrelationData.setMessage(msg);
        gmallCorrelationData.setDelayTime(delayTime);
        gmallCorrelationData.setDelay(true);

        //  将数据存到缓存
        redisTemplate.opsForValue().set(correlationId , JSON.toJSONString(gmallCorrelationData) , 10 , TimeUnit.MINUTES);

        //发送消息
        rabbitTemplate.convertAndSend(exchange , routingKey , msg , message -> {
            // 设置延迟时间
            message.getMessageProperties().setDelay(delayTime*1000);
            return message;
        },gmallCorrelationData);
        return true;
    }











}

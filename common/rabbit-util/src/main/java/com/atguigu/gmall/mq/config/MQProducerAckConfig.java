package com.atguigu.gmall.mq.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.mq.model.GmallCorrelationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback,RabbitTemplate.ReturnCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostConstruct
    public void init(){
        // this 代表当前对象
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnCallback(this);
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String acuse) {
        // ack = true 说明正确发送到交换机
        if (ack){
            System.out.println("来了来了");
            log.info("消息发送到交换机");
        }else {
            log.info("消息没有到交换机");
            //调用重实方法
            retrySendMsg(correlationData);
        }
    }


    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        // 反序列化对象输出
        System.out.println("消息主体: " + new String(message.getBody()));
        System.out.println("应答码: " + replyCode);
        System.out.println("描述：" + replyText);
        System.out.println("消息使用的交换器 exchange : " + exchange);
        System.out.println("消息使用的路由键 routing : " + routingKey);

        //获取这个CorrelationData对象Id
        String correlationDataId = (String) message.getMessageProperties().getHeaders().get("spring_returned_message_correlation");
        //因为在发送消息的时候，已经将数据存储到缓存，通过  correlationDataId 来获取缓存数据
        String strJson = (String) redisTemplate.opsForValue().get(correlationDataId);
        //消息没有到队列的时候，则会调用重试的方法
        GmallCorrelationData gmallCorrelationData = JSON.parseObject(strJson, GmallCorrelationData.class);
        //调用方法 gmallCorrelationData 这对象中，至少有，交换机，路由键，消息等
        retrySendMsg(gmallCorrelationData);
    }
    /**
     * 重试发送方法
     * @param correlationData   父类对象  它下面还有个子类对象 GmallCorrelationData
     */
    private void retrySendMsg(CorrelationData correlationData) {
        //  数据类型转换  统一转换为子类处理
        GmallCorrelationData gmallCorrelationData = (GmallCorrelationData) correlationData;
        //  获取到重试次数 初始值 0
        int retryCount = gmallCorrelationData.getRetryCount();
        //  判断
        if (retryCount>=3){
            //  不需要重试了
            log.error("重试次数已到，发送消息失败:"+JSON.toJSONString(gmallCorrelationData));
        } else {
            //  变量更新
            retryCount+=1;
            //  重新赋值重试次数 第一次重试 0->1 1->2 2->3
            gmallCorrelationData.setRetryCount(retryCount);
            System.out.println("重试次数：\t"+retryCount);

            //  更新缓存中的数据
            this.redisTemplate.opsForValue().set(gmallCorrelationData.getId(),JSON.toJSONString(gmallCorrelationData),10, TimeUnit.MINUTES);

            if (gmallCorrelationData.isDelay()){
                rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange() , gmallCorrelationData.getRoutingKey() , gmallCorrelationData.getMessage() , message -> {
                    // 设置延迟时间
                    message.getMessageProperties().setDelay(gmallCorrelationData.getDelayTime()*1000);
                    return message;
                },gmallCorrelationData);
            }else {
                //  调用发送消息方法 表示发送普通消息
                this.rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(),gmallCorrelationData.getRoutingKey(),gmallCorrelationData.getMessage(),gmallCorrelationData);

            }
        }
    }
}

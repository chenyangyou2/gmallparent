package com.atguigu.gmall.mq.rececer;

import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class ConfirmReceiver {

    @Autowired
    private RedisTemplate redisTemplate;

    // 监听消息
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm", autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm", autoDelete = "true"),
            key = {"routing.confirm"}))
    public void getMsg(String msg, Message message, Channel channel) {
        System.out.println("接收消息：" + msg);

        //消息主体 获取消息的内容
        byte[] body = message.getBody();
        System.out.println("消息内容：" + new String(body));

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    // 监听消息 不需要在设置绑定关系 配置类相当于做了绑定关系
    @SneakyThrows
    @RabbitListener(queues = DeadLetterMqConfig.queue_dead_2)
    public void getMsg1(String msg, Message message, Channel channel) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("接收时间" + simpleDateFormat.format(new Date()));
        System.out.println("接收的消息" + msg);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @SneakyThrows
    @RabbitListener(queues = DelayedMqConfig.queue_delay_1 )
    public void getMsg2(String msg, Message message, Channel channel) {

        // 使用setnx命令来解决
        String msgKey = "aelay:" + msg;
        Boolean result = redisTemplate.opsForValue().setIfAbsent(msgKey, "0", 10, TimeUnit.MINUTES);
//        if (result){
//            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            System.out.println("接收时间" + simpleDateFormat.format(new Date()));
//            System.out.println("接收的消息" + msg);
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
//        }else{
//
//        }

        if (!result){
            String status = (String) redisTemplate.opsForValue().get(msgKey);
            if ("1".equals(status)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
                return;
            }else {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                System.out.println("接收时间" + simpleDateFormat.format(new Date()));
                System.out.println("接收的消息" + msg);
                redisTemplate.opsForValue().set(msgKey , "1" );
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            }
        }


        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("接收时间" + simpleDateFormat.format(new Date()));
        System.out.println("接收的消息" + msg);

        //修改redis 中的数据
        redisTemplate.opsForValue().set(msgKey , "1" );
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
